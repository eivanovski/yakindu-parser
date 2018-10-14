package sct.parser

enum class TokenRegex(val regexStr: String) {
    Comment("//.*"),
    Spaces("\\s+"),
    StringLiteral("\"[^\"]*\""), // escaped double quotes not supported
    Word("[a-zA-Z_]\\w*"),
    Number("(0|[1-9][0-9]*)(\\.[0-9]*)?"), // no negative numbers, numbers like '.1234' not supported
    /**/;

    val regex = Regex(regexStr)

    companion object {
        fun asSequence() = values().asSequence().map { it.regexStr }
    }
}

infix fun String.matches(tokenRegex: TokenRegex) = tokenRegex.regex.matchEntire(this) != null

class Tokenizer(delimiters: Set<Char>) {
    private val regex: Regex

    init {
        var regexSeq = TokenRegex.asSequence()
        if (delimiters.isNotEmpty()) {
            val escapedChars = delimiters.asSequence()
                    .map { "\\$it" }
                    .reduce { acc, s -> "$acc$s" }
            val delimitersRegexString = "[$escapedChars]"
            regexSeq += delimitersRegexString
        }
        regex = Regex(regexSeq.reduce { acc, s -> "$acc|$s" })
    }

    fun tokenize(input: String): TokenSequence {
        var previousEnd = 0
        val array = regex.findAll(input)
                .map {
                    if (previousEnd < it.range.start) {
                        throw  UnexpectedSymbols(input.substring(previousEnd, it.range.start))
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

class UnexpectedSymbols(symbols: String) : RuntimeException("Unexpected symbols '$symbols'")

class TokenSequence
private constructor(
        private val array: Array<String>,
        private var index: Int,
        private val parent: TokenSequence?
) {
    constructor(array: Array<String>) : this(array, -1, null)

    val hasNext get() = index + 1 < array.size
    val current get() = array[index]
    fun next() = array[++index]

    fun child() = TokenSequence(array, index, this)
    fun commit() {
        if (parent != null) parent.index = this.index
    }
}

