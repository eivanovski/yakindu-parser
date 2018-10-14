package sct.parser

import kotlin.reflect.KClass
import kotlin.reflect.KParameter
import kotlin.reflect.KProperty1
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.full.primaryConstructor

class ObjBuilder<T : Any>
private constructor(
        private val klass: KClass<T>,
        private val parent: ObjBuilder<T>?
) {
    private val valueStorage = HashMap<String, Any?>()
    private val listStorage = HashMap<String, MutableList<Any>>()

    constructor(klass: KClass<T>) : this(klass, null)

    fun child() = ObjBuilder(klass, this)

    fun <V : Any?> setProperty(property: KProperty1<T, V>, value: V) {
        valueStorage[property.name] = value
    }

    fun <V : Any> addToList(property: KProperty1<T, List<V>>, value: V) {
        listStorage.computeIfAbsent(property.name) { ArrayList() }
                .add(value)
    }

    fun commit() {
        if (parent != null) {
            parent.valueStorage.putAll(valueStorage)
            listStorage.forEach { key, list ->
                parent.listStorage.computeIfAbsent(key) { ArrayList() }
                        .addAll(list)
            }
        }
    }

    fun getValue(param: KParameter): Any? {
        val name = param.name ?: return null
        val classifier = param.type.classifier
        return if ((classifier as? KClass<*>)?.isSubclassOf(List::class) == true) {
            listStorage[name] ?: ArrayList<Any>()
        } else {
            valueStorage[name]
        }
    }

    fun build(): T {
        val constructor = klass.primaryConstructor
                ?: throw RuntimeException("Primary constructor expected in class $klass")
        val params = HashMap<KParameter, Any?>()
        constructor.parameters.forEach {
            params[it] = getValue(it)
        }
        return constructor.callBy(params)
    }
}
