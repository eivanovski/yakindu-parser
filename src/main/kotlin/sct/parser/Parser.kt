package sct.parser

import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.full.companionObjectInstance


interface Parsable

data class ProtocolDetails(val keywords: Set<Keyword>, val delimiters: Set<Char>)

class TokenSequenceParser<T : Parsable>(
        private val tClass: KClass<T>,
        private val instruction: ParseInstruction<T>
) {
    internal fun collectDetails(keywords: MutableSet<Keyword>, delimiters: MutableSet<Char>, processed: MutableSet<Any>) {
        if (!processed.add(this)) return

        object : ParsingContext<T>() {
            override fun keyword(keyword: Keyword) {
                keywords.add(keyword)
            }

            override fun delimiter(delimiter: Char) {
                delimiters.add(delimiter)
            }

            override fun parseString() = NoValue<String>()
            override fun parseInt() = NoValue<Int>()
            override fun <E> parseFlag(flag: E): ParsedValue<Boolean> where E : Keyword, E : Enum<E> {
                keywords.add(flag)
                return NoValue()
            }

            override fun <E> parseEnum(enumValues: Array<E>): ParsedValue<E> where E : Keyword, E : Enum<E> {
                keywords.addAll(enumValues)
                return NoValue()
            }

            override fun <V : Parsable> parseObj(objClass: KClass<V>): ParsedValue<V> {
                getParser(objClass).collectDetails(keywords, delimiters, processed)
                return NoValue()
            }

            override fun <V : Any> ParsedValue<V>.toProp(property: KProperty1<T, V?>) {}

            override fun <V : Any> ParsedValue<V>.toList(property: KProperty1<T, List<V>>) {}

            private val visitingBlockMatcher = object : BlockMatcher<T> {
                override fun or(block: ParseInstruction<T>): BlockMatcher<T> {
                    block()
                    return this
                }
            }

            override fun Char.blockMatcher() = visitingBlockMatcher
        }.instruction()
    }

    fun parse(tokens: TokenSequence): T? {
        val objBuilder = ObjBuilder(tClass)
        if (ParsingContextImpl(objBuilder, tokens).execute(instruction))
            return objBuilder.build()
        return null
    }
}

abstract class Parser<T : Parsable>
private constructor(
        val parsers: List<TokenSequenceParser<out T>>
) {
    constructor(vararg subClassParsers: Parser<out T>) : this(subClassParsers.asIterable().flatMap { it.parsers })
    constructor(parser: TokenSequenceParser<T>) : this(listOf(parser))

    val parser = this
    private val protocolDetails by lazy { collectDetails(HashSet(), HashSet(), HashSet()) }
    private val tokenizer by lazy { Tokenizer(protocolDetails.delimiters) }

    internal fun collectDetails(keywords: MutableSet<Keyword>, delimiters: MutableSet<Char>, processed: MutableSet<Any>): ProtocolDetails {
        parsers.forEach { it.collectDetails(keywords, delimiters, processed) }
        return ProtocolDetails(keywords, delimiters)
    }

    fun parse(input: String): T? {
        val tokens = tokenizer.tokenize(input)
        return parse(tokens)
    }

    fun parse(tokens: TokenSequence) = parsers.asSequence()
            .map { it.parse(tokens) }
            .find { it != null }
}


@Suppress("UNCHECKED_CAST")
fun <T : Parsable> getParser(klass: KClass<T>): Parser<T> {
    val companion = klass.companionObjectInstance ?: throw RuntimeException("companion expected $klass")
    return companion as? Parser<T>
            ?: throw RuntimeException("companion expected to extend ${Parser::class}")
}

inline fun <reified T : Parsable> getParser() = getParser(T::class)

inline fun <reified T : Parsable> description(noinline lambda: ParseInstruction<T>) = TokenSequenceParser(T::class, lambda)