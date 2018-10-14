package sct.parser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class TokenizerTest {
    @Test
    fun noDelimitersTest() {
        val sequence = Tokenizer(setOf())
                .tokenize("aaa bbb ccc ddd")

        assertEquals("aaa", sequence.next())
        assertEquals("bbb", sequence.next())
        assertEquals("ccc", sequence.next())
        assertEquals("ddd", sequence.next())
        assertFalse(sequence.hasNext)
    }

    @Test(expected = UnexpectedSymbols::class)
    fun unknownDelimitersTest() {
        Tokenizer(setOf(':')).tokenize("aaa:bbb , ccc:ddd")
    }

    @Test
    fun delimitersTest() {
        val sequence = Tokenizer(setOf(',', ':'))
                .tokenize("aaa:bbb , ccc:ddd")

        assertEquals("aaa", sequence.next())
        assertEquals(":", sequence.next())
        assertEquals("bbb", sequence.next())
        assertEquals(",", sequence.next())
        assertEquals("ccc", sequence.next())
        assertEquals(":", sequence.next())
        assertEquals("ddd", sequence.next())
        assertFalse(sequence.hasNext)
    }

    @Test
    fun commentTest() {
        val input = """
aaa bbb ccc //ddd
fff
"""
        val sequence = Tokenizer(setOf()).tokenize(input)

        assertEquals("aaa", sequence.next())
        assertEquals("bbb", sequence.next())
        assertEquals("ccc", sequence.next())
        assertEquals("fff", sequence.next())
        assertFalse(sequence.hasNext)
    }

    @Test
    fun stringLiteralTest() {
        val input = """
aaa = "bbb"
"""
        val sequence = Tokenizer(setOf('=')).tokenize(input)

        assertEquals("aaa", sequence.next())
        assertEquals("=", sequence.next())
        assertEquals("\"bbb\"", sequence.next())
        assertFalse(sequence.hasNext)
    }

    @Test
    fun commentInStringLiteralTest() {
        val input = """
aaa = "//bbb"
"""
        val sequence = Tokenizer(setOf('=')).tokenize(input)

        assertEquals("aaa", sequence.next())
        assertEquals("=", sequence.next())
        assertEquals("\"//bbb\"", sequence.next())
        assertFalse(sequence.hasNext)
    }

    @Test
    fun commentedStringLiteralTest() {
        val input = """
aaa = //"bbb"
"""
        val sequence = Tokenizer(setOf('=')).tokenize(input)

        assertEquals("aaa", sequence.next())
        assertEquals("=", sequence.next())
        assertFalse(sequence.hasNext)
    }
}