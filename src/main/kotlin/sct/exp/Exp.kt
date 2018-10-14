package sct.exp

import sct.parser.Keyword
import sct.parser.TokenSequence
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KParameter
import kotlin.reflect.KProperty1
import kotlin.reflect.full.primaryConstructor

class ObjMap<T : Any>
private constructor(
        val kFunction: KFunction<T>
) {
    constructor(klass: KClass<T>) : this(klass.primaryConstructor
            ?: throw RuntimeException("Class ${klass.qualifiedName} expected to have primary constructor")
    )

    fun build(): T {
        val arguments = HashMap<KParameter, Any?>()
        // todo fill map
        return kFunction.callBy(arguments)
    }
}

interface SeqParser<out T : Any> {
    fun parseSequence(tokens: TokenSequence): T
}

class SeqParserImpl<T : Any>(
        val klass: KClass<T>,
        val instruction: InitContext<T>.() -> Unit
) : SeqParser<T> {
    override fun parseSequence(tokens: TokenSequence): T {
        val objMap = ObjMap(klass)
        val context = InitContextImpl(objMap)
        context.instruction() // todo check
        return objMap.build()
    }
}

interface InitContext<T> {
    fun keyword(keyword: Keyword)
    fun delimiter(delimiter: Char)
    fun <V : Any> captureObj(property: KProperty1<T, V?>, vararg parsers: SeqParser<V>)
}

class InitContextImpl<T : Any>(
        val objBuilder: ObjMap<T>
) : InitContext<T> {
    override fun keyword(keyword: Keyword) {
    }

    override fun delimiter(delimiter: Char) {
    }

    override fun <V : Any> captureObj(property: KProperty1<T, V?>, vararg parsers: SeqParser<V>) {
    }
}