package sct.model

import sct.parser.*


enum class SctKeyword : Keyword {
    Event, Operation
}

enum class SctType : Keyword {
    Void, Boolean, Integer, Real, String
}

enum class EventDirection : Keyword {
    In, Out
}

data class Event(val direction: EventDirection, val name: String, val type: SctType?) : Parsable {
    companion object : Parser<Event>(description {
        Event::direction.parse().asEnum()
        keyword(SctKeyword.Event)
        Event::name.parse().asString()
        '?' of {
            delimiter(':')
            Event::type.parse().asEnum()
        }
    })
}

data class Operation(
        val name: String,
        val arguments: List<Argument>,
        val returnType: SctType?
) : Parsable {
    companion object : Parser<Operation>(description {
        keyword(SctKeyword.Operation)
        Operation::name.parse().asString()
        delimiter('(')
        '?' of {
            Operation::arguments.parseElement().asObj()
            '*' of {
                delimiter(',')
                Operation::arguments.parseElement().asObj()
            }
        }
        delimiter(')')
        '?' of {
            delimiter(':')
            Operation::returnType.parse().asEnum()
        }
    })
}

data class Argument(val name: String, val type: SctType) : Parsable {
    companion object : Parser<Argument>(description {
        Argument::name.parse().asString()
        delimiter(':')
        Argument::type.parse().asEnum()
    })
}

fun main(args: Array<String>) {
    println(Event.parse("in event Aaaa"))
    println(Event.parse("out event Bbbb"))
    println(Event.parse("in event Aaaa : string"))
    println(Operation.parse("operation doSomething(aaa:integer, bbb:boolean):string"))
}

