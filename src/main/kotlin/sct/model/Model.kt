package sct.model

import sct.parser.Keyword
import sct.parser.Parsable
import sct.parser.Parser
import sct.parser.description


enum class SctKeyword : Keyword {
    Interface, Internal,
    Event, Var, Operation,
    Readonly
}

enum class SctType : Keyword {
    Void, Boolean, Integer, Real, String
}

enum class EventDirection : Keyword {
    In, Out
}

enum class BooleanValues : Keyword {
    False, True
}

data class StateMachineSpec(val internal: InternalSpec?, val interfaces: List<InterfaceSpec>) : Parsable {
    companion object : Parser<StateMachineSpec>(description {
        '*' of { parseObj<InterfaceSpec>() toList StateMachineSpec::interfaces }
        '?' of {
            parseObj<InternalSpec>() toProp StateMachineSpec::internal
            '*' of { parseObj<InterfaceSpec>() toList StateMachineSpec::interfaces }
        }
    })
}

data class InternalSpec(val events: List<Event>, val variables: List<Variable>, val operations: List<Operation>, val localReactions: List<LocalReaction>) : Parsable {
    companion object : Parser<InternalSpec>(description {
        keyword(SctKeyword.Internal)
        delimiter(':')
        '*' of { parseObj<Event>() toList InternalSpec::events } or
                { parseObj<Variable>() toList InternalSpec::variables } or
                { parseObj<Operation>() toList InternalSpec::operations } or
                { parseObj<LocalReaction>() toList InternalSpec::localReactions }
    })
}

data class InterfaceSpec(val name: String, val events: List<Event>, val variables: List<Variable>, val operations: List<Operation>) : Parsable {
    companion object : Parser<InterfaceSpec>(description {
        keyword(SctKeyword.Interface)
        parseString() toProp InterfaceSpec::name
        delimiter(':')
        '*' of { parseObj<Event>() toList InterfaceSpec::events } or
                { parseObj<Variable>() toList InterfaceSpec::variables } or
                { parseObj<Operation>() toList InterfaceSpec::operations }
    })
}

data class Event(val direction: EventDirection, val name: String, val type: SctType?) : Parsable {
    companion object : Parser<Event>(description {
        parseEnum<EventDirection>() toProp Event::direction
        keyword(SctKeyword.Event)
        parseString() toProp Event::name
        '?' of {
            delimiter(':')
            parseEnum<SctType>() toProp Event::type
        }
    })
}

data class Variable(val readonly: Boolean, val name: String, val type: SctType, val initExpr: Any?) : Parsable {
    companion object : Parser<Variable>(description {
        keyword(SctKeyword.Var)
        parseFlag(SctKeyword.Readonly) toProp Variable::readonly
        parseString() toProp Variable::name
        delimiter(':')
        parseEnum<SctType>() toProp Variable::type
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
        parseString() toProp Operation::name
        delimiter('(')
        '?' of {
            parseObj<Argument>() toList Operation::arguments
            '*' of {
                delimiter(',')
                parseObj<Argument>() toList Operation::arguments
            }
        }
        delimiter(')')
        '?' of {
            delimiter(':')
            parseEnum<SctType>() toProp Operation::returnType
        }
    })
}

data class LocalReaction(val event: String, val guard: Any?, val action: Any) : Parsable {
    companion object : Parser<LocalReaction>(description {

    })
}

data class Argument(val name: String, val type: SctType) : Parsable {
    companion object : Parser<Argument>(description {
        parseString() toProp Argument::name
        delimiter(':')
        parseEnum<SctType>() toProp Argument::type
    })
}

data class QualifiedId(val ids: List<String>) : Parsable {
    companion object : Parser<QualifiedId>(description {
        parseString() toList QualifiedId::ids
        '*' of {
            delimiter('.')
            parseString() toList QualifiedId::ids
        }
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

