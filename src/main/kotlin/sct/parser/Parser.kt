package parser

import kotlin.reflect.KFunction
import kotlin.reflect.KProperty1

class Parser<T>(
        val objConstructor: KFunction<T>,
        val instruction: ParseInstruction<T>
) {
    fun parse(input: String): T? = parse(Tokenizer.tokenize(input))

    fun parse(tokenSequence: TokenSequence): T? {
        val builder = ObjBuilder(objConstructor)
        if (ParsingContext.tryToPerform(instruction, tokenSequence, builder))
            return builder.build()
        else
            return null
    }
}

class ParsingContext<T>
private constructor(
        private val tokens: TokenSequence,
        private val builder: ObjBuilder<T>
) {
    private var failed = false

    companion object {
        fun <T> tryToPerform(instruction: ParseInstruction<T>, tokens: TokenSequence, builder: ObjBuilder<T>): Boolean {
            with(ParsingContext(tokens.child(), builder.child())) {
                instruction()
                if (!failed) {
                    this.tokens.commit()
                    this.builder.commit()
                }
                return !failed
            }
        }
    }

    fun check(): Boolean {
        failed = failed || !tokens.tryProgress()
        return !failed
    }

    fun check(predicate: (String) -> Boolean): Boolean {
        failed = failed || !tokens.tryProgress() || !predicate.invoke(tokens.current())
        return !failed
    }

    fun keyword(keyword: String) {
        check { it == keyword }
    }

    fun delimiter(delimiter: Char) {
        check { it == delimiter.toString() }
    }

    fun capture(property: KProperty1<T, String?>) {
        if (check()) {
            builder.set(property, tokens.current())
        }
    }

    fun optional(instruction: ParseInstruction<T>) {
        tryToPerform(instruction, tokens, builder)
    }
}

typealias ParseInstruction<T> = ParsingContext<T>.() -> Unit

fun <T> parser(objConstructor: KFunction<T>, instruction: ParseInstruction<T>) = Parser(objConstructor, instruction)

data class InEvent(val name: String, val type: String?)

val inEvent = parser(::InEvent) {
    keyword("in")
    keyword("event")
    capture(InEvent::name)
    optional {
        delimiter(':')
        capture(InEvent::type)
    }
}

data class Operation(val name: String, val returnType: String?, val arguments: List<String>)
val operation = parser(::Operation) {
    keyword("operation")
    capture(Operation::name)
    delimiter('(')
    delimiter(')')
}

fun main(args: Array<String>) {
//    println(inEvent.parse("in event Aaaaa"))
//    println(inEvent.parse("in event Bbbb : integer"))

    println(operation.parse("operation someOperation()"))
}

