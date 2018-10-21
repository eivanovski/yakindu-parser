package sct.parser

import kotlin.reflect.KClass
import kotlin.reflect.KProperty1


interface BlockMatcher<T : Parsable> {
    infix fun or(block: ParseInstruction<T>): BlockMatcher<T>
}

typealias ParseInstruction<T> = ParsingContext<T>.() -> Unit

sealed class ParsedValue<T : Any> {
    abstract infix fun <S : Any> transform(lambda: (T) -> S?): ParsedValue<S>
    abstract fun withValue(lambda: (T) -> Unit)

    companion object {
        fun <V : Any> wrap(value: V?) = when (value) {
            null -> NoValue<V>()
            else -> ActualParsedValue(value)
        }
    }

    class ActualParsedValue<T : Any>(val value: T) : ParsedValue<T>() {
        override fun <S : Any> transform(lambda: (T) -> S?) = wrap(lambda.invoke(value))
        override fun withValue(lambda: (T) -> Unit) = lambda.invoke(value)
    }
}

class NoValue<T : Any> : ParsedValue<T>() {
    override fun <S : Any> transform(lambda: (T) -> S?) = NoValue<S>()
    override fun withValue(lambda: (T) -> Unit) {}
}

abstract class ParsingContext<T : Parsable> {
    abstract fun keyword(keyword: Keyword)
    abstract fun delimiter(delimiter: Char)

    abstract fun parseString(): ParsedValue<String>
    abstract fun parseInt(): ParsedValue<Int>
    abstract fun <E> parseFlag(flag: E): ParsedValue<Boolean> where E : Keyword, E : Enum<E>
    inline fun <reified E> parseEnum(): ParsedValue<E>
            where E : Keyword,
                  E : Enum<E> =
            parseEnum(enumValues())

    inline fun <reified V : Parsable> parseObj(): ParsedValue<V> = parseObj(V::class)

    abstract infix fun <V : Any> ParsedValue<V>.toProp(property: KProperty1<T, V?>)
    abstract infix fun <V : Any> ParsedValue<V>.toList(property: KProperty1<T, List<V>>)

    infix fun Char.of(block: ParseInstruction<T>): BlockMatcher<T> = blockMatcher().or(block)

    @PublishedApi
    internal abstract fun <E> parseEnum(enumValues: Array<E>): ParsedValue<E>
            where E : Keyword,
                  E : Enum<E>

    @PublishedApi
    internal abstract fun <V : Parsable> parseObj(objClass: KClass<V>): ParsedValue<V>

    protected abstract fun Char.blockMatcher(): BlockMatcher<T>
}

class ParsingContextImpl<T : Parsable>(
        initObjBuilder: ObjBuilder<T>,
        initTokens: TokenSequence
) : ParsingContext<T>() {
    private val objBuilder = initObjBuilder.child()
    private val tokens = initTokens.child()
    private var ok = true

    fun tryExecute(block: ParseInstruction<T>) = ParsingContextImpl(objBuilder, tokens).execute(block)

    fun execute(block: ParseInstruction<T>): Boolean {
        this.block()
        if (ok) {
            objBuilder.commit()
            tokens.commit()
        }
        return ok
    }

    private fun check(predicate: (String) -> Boolean): Boolean {
        ok = ok && tokens.hasNext && predicate.invoke(tokens.next())
        return ok
    }

    private fun checkEquals(toCheck: String) = check { it == toCheck }

    override fun keyword(keyword: Keyword) {
        checkEquals(keyword.keyword)
    }

    override fun delimiter(delimiter: Char) {
        checkEquals(delimiter.toString())
    }

    private fun checkAndParse(predicate: (String) -> Boolean): ParsedValue<String> {
        return when {
            check(predicate) -> ParsedValue.wrap(tokens.current)
            else -> NoValue()
        }
    }

    override fun parseString(): ParsedValue<String> = checkAndParse { it matches TokenRegex.Word }
    override fun parseInt(): ParsedValue<Int> = checkAndParse { it matches TokenRegex.Number } transform { it.toInt() }
    override fun <E> parseFlag(flag: E): ParsedValue<Boolean>
            where E : Keyword,
                  E : Enum<E> =
            ParsedValue.wrap(tryExecute { keyword(flag) })

    override fun <E> parseEnum(enumValues: Array<E>): ParsedValue<E> where E : Keyword, E : Enum<E> {
        return checkAndParse { it matches TokenRegex.Word } transform { value ->
            enumValues.asSequence()
                    .find { it.keyword == value }
                    .also { if (it == null) ok = false }
        }
    }

    override fun <V : Parsable> parseObj(objClass: KClass<V>): ParsedValue<V> {
        if (ok) {
            val obj = getParser(objClass).parse(tokens)
            if (obj == null) {
                ok = false
            }
            return ParsedValue.wrap(obj)
        }
        return NoValue()
    }

    override fun <V : Any> ParsedValue<V>.toProp(property: KProperty1<T, V?>) {
        withValue { objBuilder.setProperty(property, it) }
    }

    override fun <V : Any> ParsedValue<V>.toList(property: KProperty1<T, List<V>>) {
        withValue { objBuilder.addToList(property, it) }
    }

    override fun Char.blockMatcher(): BlockMatcher<T> {
        return if (ok) when (this) {
            '?' -> BlockMatcherImpl(0..1)
            '*' -> BlockMatcherImpl(0..Int.MAX_VALUE)
            '+' -> BlockMatcherImpl(1..Int.MAX_VALUE)
            else -> throw RuntimeException()
        } else
            dummyBlockMatcher
    }


    private val dummyBlockMatcher = object : BlockMatcher<T> {
        override fun or(block: ParseInstruction<T>): BlockMatcher<T> = this
    }

    private inner class BlockMatcherImpl(val range: IntRange) : BlockMatcher<T> {
        val blocks = ArrayList<ParseInstruction<T>>()

        var matchCount = 0

        override fun or(block: ParseInstruction<T>): BlockMatcher<T> {
            blocks += block
            tryToMatch()
            return this
        }

        fun tryToMatch() {
            mainLoop@
            while (matchCount < range.endInclusive) {
                for (block in blocks) {
                    if (tryExecute(block)) {
                        matchCount++
                        continue@mainLoop
                    }
                }
                break
            }
            ok = matchCount in range
        }
    }
}
