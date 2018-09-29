package sct.model

import sct.parser.parser

data class InEvent(val name: String, val type: String?) {
    companion object {
        val parser = parser(::InEvent) {
            keyword("in")
            keyword("event")
            capture(InEvent::name)
            optional {
                delimiter(':')
                capture(InEvent::type)
            }
        }
    }
}

data class Operation(val name: String, val returnType: String?, val arguments: List<String>) {
    companion object {
        val parser = parser(::Operation) {
            keyword("operation")
            capture(Operation::name)
            delimiter('(')
            delimiter(')')
        }
    }
}

fun main(args: Array<String>) {
//    println(inEvent.parse("in event Aaaaa"))
//    println(inEvent.parse("in event Bbbb : integer"))

    println(Operation.parser.parse("operation someOperation()"))
}

