package sct.parser

enum class TokenRegex(
        val regexStr: String
) {
    Comment("//.*"),
    Spaces("\\s+"),
    StringLiteral("\"[^\"]*\""),
    Word("[a-zA-Z_]\\w*"),
    Number("[+-]?\\d+(\\.[0-9]*)?"),
    Delimiter("[.,:;!()/\\[\\]]"),
    /**/;

    val regex = Regex(regexStr)
}

infix fun String.matches(tokenRegex: TokenRegex) = tokenRegex.regex.matchEntire(this) != null

object Tokenizer {
    val regex = Regex(TokenRegex.values().map { it.regexStr }.reduce { acc, s -> "$acc|$s" })

    fun tokenize(input: String): TokenSequence {
        var previousEnd = 0
        val array = regex.findAll(input)
                .map {
                    if (previousEnd < it.range.start) {
                        throw  RuntimeException("'" + input.substring(previousEnd, it.range.start) + "'")
                    }
                    previousEnd = it.range.endInclusive + 1
                    it.value
                }
                .filterNot { it matches TokenRegex.Comment }
                .filterNot { it matches TokenRegex.Spaces }
                .toList()
                .toTypedArray()
        return TokenSequence(array)
    }
}

class TokenSequence
private constructor(
        private val array: Array<String>,
        private var index: Int,
        private val parent: TokenSequence?
) {
    constructor(array: Array<String>) : this(array, -1, null)

    fun child() = TokenSequence(array, index, this)
    fun tryProgress() = ++index < array.size
    fun current() = array[index]
    fun commit() {
        if (parent != null) parent.index = this.index
    }
}

