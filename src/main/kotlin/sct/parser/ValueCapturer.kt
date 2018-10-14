package sct.parser

import kotlin.reflect.KProperty1

interface PropertyWrap<T : Parsable, V : Any> {
    fun acceptValue(objBuilder: ObjBuilder<T>, value: V)
}

class SimplePropertyWrap<T : Parsable, V : Any>(val property: KProperty1<T, V?>) : PropertyWrap<T, V> {
    override fun acceptValue(objBuilder: ObjBuilder<T>, value: V) = objBuilder.setProperty(property, value)
}

class ListPropertyWrap<T : Parsable, V : Any>(val property: KProperty1<T, List<V>>) : PropertyWrap<T, V> {
    override fun acceptValue(objBuilder: ObjBuilder<T>, value: V) = objBuilder.addToList(property, value)
}

fun <T : Parsable, V : Any> KProperty1<T, V?>.parse(): PropertyWrap<T, V> = SimplePropertyWrap(this)
fun <T : Parsable, V : Any> KProperty1<T, List<V>>.parseElement(): PropertyWrap<T, V> = ListPropertyWrap(this)
