package sct.parser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

enum class TestKeywords : Keyword {
    Name, Hold
}

data class TestNameHolder(val nameObj: NamedTestClass) : Parsable {
    companion object : Parser<TestNameHolder>(description {
        keyword(TestKeywords.Hold)
        delimiter('(')
        TestNameHolder::nameObj.parse().asObj()
        delimiter(')')
    })
}

data class NamedTestClass(val name: String) : Parsable {
    companion object : Parser<NamedTestClass>(description {
        keyword(TestKeywords.Name)
        delimiter('=')
        NamedTestClass::name.parse().asString()
    })
}

sealed class TestExpr : Parsable {
    companion object : Parser<TestExpr>(
            SummExpr.parser,
            TestSingleExpr.parser
    )
}

sealed class TestSingleExpr : TestExpr() {
    companion object : Parser<TestSingleExpr>(
            LiteralExpr.parser,
            ParenthesisExpr.parser
    )
}

data class LiteralExpr(val value: Int) : TestSingleExpr() {
    companion object : Parser<LiteralExpr>(description {
        LiteralExpr::value.parse().asInt()
    })
}

data class ParenthesisExpr(val expr: TestExpr) : TestSingleExpr() {
    companion object : Parser<ParenthesisExpr>(description {
        delimiter('(')
        ParenthesisExpr::expr.parse().asObj()
        delimiter(')')
    })
}

data class SummExpr(val summands: List<TestSingleExpr>) : TestExpr() {
    companion object : Parser<SummExpr>(description {
        SummExpr::summands.parseElement().asObj()
        '+' of {
            delimiter('+')
            SummExpr::summands.parseElement().asObj()
        }
    })
}

class ParserTest {
    @Test
    fun test() {
        val obj = NamedTestClass.parse("name = Vasya")
        assertNotNull(obj)
        assertEquals("Vasya", obj?.name)
    }

    @Test
    fun objTest() {
        val obj = TestNameHolder.parse("hold(name = Vasya)")
        assertNotNull(obj)
        assertNotNull(obj?.nameObj)
        assertEquals("Vasya", obj?.nameObj?.name)
    }

    @Test
    fun recursiveParser() {
        val obj = TestExpr.parse("(10 + 20 + 30 + 40)")
        assertNotNull(obj)
    }
}