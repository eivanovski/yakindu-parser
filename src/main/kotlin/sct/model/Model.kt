package sct.model

import sct.parser.*


enum class SctKeyword : Keyword {
    Interface, Event, Var, Operation,
    Readonly
}

enum class SctType : Keyword {
    Void, Boolean, Integer, Real, String
}

enum class EventDirection : Keyword {
    In, Out
}

data class InterfaceSpec(val name: String, val events: List<Event>, val variables: List<Variable>, val operations: List<Operation>) : Parsable {
    companion object : Parser<InterfaceSpec>(description {
        keyword(SctKeyword.Interface)
        InterfaceSpec::name.parse().asString()
        delimiter(':')
        '*' of { InterfaceSpec::events.parseElement().asObj() } or
                { InterfaceSpec::variables.parseElement().asObj() } or
                { InterfaceSpec::operations.parseElement().asObj() }
    })
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

data class Variable(val readonly: Boolean, val name: String, val type: SctType, val initExpr: Any?) : Parsable {
    companion object : Parser<Variable>(description {
        keyword(SctKeyword.Var)
        Variable::readonly.parse().asFlag(SctKeyword.Readonly)
        Variable::name.parse().asString()
        delimiter(':')
        Variable::type.parse().asEnum()
        // todo: init expr
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
    val spec =
            """
interface SomeInterface:
in event Aaaa
out event Bbbb
in event Aaaa : string
operation doSomething(aaa:integer, bbb:boolean):string
var readonly someVar : integer
var someVar : string
""".trimIndent()

    println(InterfaceSpec.parse(spec))
}

