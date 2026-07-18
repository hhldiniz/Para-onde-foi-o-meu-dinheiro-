package com.hhldiniz.praondefoiomeudinheiro.data.local

import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.ByteArrayInputStream

class CsvParserTest {

    private fun stream(content: String) =
        ByteArrayInputStream(content.toByteArray(Charsets.UTF_8))

    @Test
    fun parse_simpleRows() {
        val input = "a,b,c\n1,2,3\n4,5,6"
        val result = CsvParser.parse(stream(input))
        assertEquals(3, result.size)
        assertEquals(listOf("a", "b", "c"), result[0])
        assertEquals(listOf("1", "2", "3"), result[1])
        assertEquals(listOf("4", "5", "6"), result[2])
    }

    @Test
    fun parse_skipsBlankLines() {
        val input = "a,b\n\n1,2\n\n3,4"
        val result = CsvParser.parse(stream(input))
        assertEquals(3, result.size)
        assertEquals(listOf("a", "b"), result[0])
        assertEquals(listOf("1", "2"), result[1])
        assertEquals(listOf("3", "4"), result[2])
    }

    @Test
    fun parse_handlesQuotedFields() {
        val input = "\"hello, world\",normal,\"with \"\"quote\"\"\""
        val result = CsvParser.parse(stream(input))
        assertEquals(1, result.size)
        assertEquals("hello, world", result[0][0])
        assertEquals("normal", result[0][1])
        assertEquals("with \"quote\"", result[0][2])
    }

    @Test
    fun parse_trimsFields() {
        val input = "  a  , b ,c"
        val result = CsvParser.parse(stream(input))
        assertEquals(listOf("a", "b", "c"), result[0])
    }

    @Test
    fun parse_emptyStream_returnsEmpty() {
        val result = CsvParser.parse(stream(""))
        assertEquals(0, result.size)
    }
}
