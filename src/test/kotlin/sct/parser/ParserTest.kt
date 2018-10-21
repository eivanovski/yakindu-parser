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
        parseObj<NamedTestClass>() toProp TestNameHolder::nameObj
        delimiter(')')
    })
}

data class NamedTestClass(val name: String) : Parsable {
    companion object : Parser<NamedTestClass>(description {
        keyword(TestKeywords.Name)
        delimiter('=')
        parseString() toProp NamedTestClass::name
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
        parseInt() toProp LiteralExpr::value
    })
}

data class ParenthesisExpr(val expr: TestExpr) : TestSingleExpr() {
    companion object : Parser<ParenthesisExpr>(description {
        delimiter('(')
        parseObj<TestExpr>() toProp ParenthesisExpr::expr
        delimiter(')')
    })
}

data class SummExpr(val summands: List<TestSingleExpr>) : TestExpr() {
    companion object : Parser<SummExpr>(description {
        parseObj<TestSingleExpr>() toList SummExpr::summands
        '+' of {
            delimiter('+')
            parseObj<TestSingleExpr>() toList SummExpr::summands
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