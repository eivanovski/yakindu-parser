package sct.parser

import org.junit.Assert.*
import org.junit.Test
import java.lang.reflect.InvocationTargetException

data class NotNullFieldClass(val aaa: String)
data class NullableFieldClass(val aaa: String, val bbb: String?)
data class ObjWithList(val list: List<String>)

class ObjBuilderTest {
    @Test
    fun simpleTest() {
        val builder = ObjBuilder(NotNullFieldClass::class)
        builder.setProperty(NotNullFieldClass::aaa, "bbb")

        val obj = builder.build()
        assertNotNull(obj)
        assertEquals("bbb", obj.aaa)
    }

    @Test(expected = InvocationTargetException::class)
    fun uninitializedField() {
        ObjBuilder(NotNullFieldClass::class).build()
    }

    @Test
    fun nullableField() {
        val builder = ObjBuilder(NullableFieldClass::class)
        builder.setProperty(NullableFieldClass::aaa, "bbb")

        val obj = builder.build()
        assertNotNull(obj)
        assertEquals("bbb", obj.aaa)
        assertNull(obj.bbb)
    }

    @Test
    fun listTest() {
        val builder = ObjBuilder(ObjWithList::class)
        builder.addToList(ObjWithList::list, "aaa")
        builder.addToList(ObjWithList::list, "bbb")
        builder.addToList(ObjWithList::list, "ccc")

        val obj = builder.build()
        assertEquals(3, obj.list.size)
        assertEquals("aaa", obj.list[0])
        assertEquals("bbb", obj.list[1])
        assertEquals("ccc", obj.list[2])
    }

    @Test
    fun emptyListTest() {
        val builder = ObjBuilder(ObjWithList::class)
        val obj = builder.build()
        assertTrue(obj.list.isEmpty())
    }
}