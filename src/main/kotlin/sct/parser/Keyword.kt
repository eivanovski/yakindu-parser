package sct.parser

interface Keyword {
    val name: String
    val keyword get() = name.toLowerCase()
}
