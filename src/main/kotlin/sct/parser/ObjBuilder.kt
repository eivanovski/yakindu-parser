package sct.parser

import kotlin.reflect.KFunction
import kotlin.reflect.KParameter
import kotlin.reflect.KProperty1

class ObjBuilder<T>
private constructor(
        private val objConstructor: KFunction<T>,
        private val parent: ObjBuilder<T>?
) {
    private val valueStorage = HashMap<String, Any?>()
    private val listStorage = HashMap<String, MutableList<Any>>()

    constructor(objConstructor: KFunction<T>) : this(objConstructor, null)

    fun child() = ObjBuilder(objConstructor, this)

    fun <V : Any?> set(property: KProperty1<T, V>, value: V) {
        valueStorage.put(property.name, value)
    }

    fun <V : Any> addTo(property: KProperty1<T, List<V>>, value: V) {
        listStorage.computeIfAbsent(property.name) { ArrayList() }
                .add(value)
    }

    fun commit() {
        if (parent != null) {
            parent.valueStorage.putAll(valueStorage)
            listStorage.forEach { key, list ->
                val parentList = parent.listStorage[key]
                if (parentList != null)
                    parentList.addAll(list)
                else
                    parent.listStorage[key] = list
            }
        }
    }

    fun getParamVal(name: String?): Any? {
        if (name == null) return null

        if (valueStorage.containsKey(name)) return valueStorage[name]

        val list = listStorage[name]
        if (list != null)
            return list
        else
            return emptyList<Any>()
    }

    fun build(): T {
        val params = HashMap<KParameter, Any?>()
        objConstructor.parameters.forEach {
            params[it] = getParamVal(it.name)
        }
        return objConstructor.callBy(params)
    }
}
