package sct.parser

import kotlin.reflect.KClass


interface BlockMatcher<T : Parsable> {
    fun or(block: ParseInstruction<T>): BlockMatcher<T> = this
}

typealias ParseInstruction<T> = ParsingContext<T>.() -> Unit

abstract class ParsingContext<T : Parsable> {
    abstract fun keyword(keyword: Keyword)
    abstract fun delimiter(delimiter: Char)

    abstract fun PropertyWrap<T, String>.asString()
    abstract fun PropertyWrap<T, Int>.asInt()
    inline fun <reified E> PropertyWrap<T, E>.asEnum() where E : Keyword, E : Enum<E> = parseEnum(enumValues(), this)
    inline fun <reified V> PropertyWrap<T, V>.asObj() where V : Parsable = parseObj(V::class, this)

    infix fun Char.of(block: ParseInstruction<T>): BlockMatcher<T> = blockMatcher().or(block)


    @PublishedApi
    internal abstract fun <E> parseEnum(enumValues: Array<E>, target: PropertyWrap<T, E>) where E : Keyword, E : Enum<E>

    @PublishedApi
    internal abstract fun <V : Parsable> parseObj(objClass: KClass<V>, target: PropertyWrap<T, V>)

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

    override fun PropertyWrap<T, String>.asString() {
        if (check { it matches TokenRegex.Word })
            this.acceptValue(objBuilder, tokens.current)
    }

    override fun PropertyWrap<T, Int>.asInt() {
        if (check { it matches TokenRegex.Number })
            this.acceptValue(objBuilder, tokens.current.toInt())
    }

    override fun <E> parseEnum(enumValues: Array<E>, target: PropertyWrap<T, E>) where E : Keyword, E : Enum<E> {
        if (check { it matches TokenRegex.Word }) {
            val matchedEnumValue = enumValues.asSequence().find { it.keyword == tokens.current }
            if (matchedEnumValue != null) {
                target.acceptValue(objBuilder, matchedEnumValue)
            } else {
                ok = false
            }
        }
    }

    override fun <V : Parsable> parseObj(objClass: KClass<V>, target: PropertyWrap<T, V>) {
        if (ok) {
            val obj = getParser(objClass).parse(tokens)
            if (obj != null) target.acceptValue(objBuilder, obj)
        }
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


    private val dummyBlockMatcher = object : BlockMatcher<T> {}

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
