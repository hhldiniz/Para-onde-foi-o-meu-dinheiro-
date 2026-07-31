package com.hhldiniz.praondefoiomeudinheiro.data.local.zip

import kotlin.random.Random
import org.junit.Assert.assertArrayEquals
import org.junit.Test
import java.util.zip.Deflater

/**
 * Cross-checks the pure-Kotlin [RawInflate] decoder (the wasmJs `inflateRaw`
 * actual, since browsers have no synchronous raw-DEFLATE API) against
 * `java.util.zip.Deflater(level, nowrap = true)` as the oracle: whatever the
 * JDK compresses, our decoder must reproduce byte-for-byte. Covers stored,
 * fixed-Huffman, and dynamic-Huffman blocks (selected via input size/content
 * and compression level) plus back-references long/near enough to exercise
 * overlapping copies.
 */
class RawInflateTest {

    private fun deflate(input: ByteArray, level: Int = Deflater.DEFAULT_COMPRESSION): ByteArray {
        val deflater = Deflater(level, true)
        deflater.setInput(input)
        deflater.finish()
        val output = java.io.ByteArrayOutputStream()
        val buffer = ByteArray(1024)
        while (!deflater.finished()) {
            val n = deflater.deflate(buffer)
            output.write(buffer, 0, n)
        }
        deflater.end()
        return output.toByteArray()
    }

    private fun assertRoundTrip(input: ByteArray, level: Int = Deflater.DEFAULT_COMPRESSION) {
        val compressed = deflate(input, level)
        val decompressed = RawInflate.inflate(compressed, input.size)
        assertArrayEquals(input, decompressed)
    }

    @Test
    fun emptyInput() {
        assertRoundTrip(ByteArray(0))
    }

    @Test
    fun shortLiteralOnlyInput() {
        assertRoundTrip("hi".encodeToByteArray())
    }

    @Test
    fun storedBlock_noCompression() {
        // Level 0 forces STORED blocks.
        assertRoundTrip("The quick brown fox jumps over the lazy dog.".repeat(20).encodeToByteArray(), Deflater.NO_COMPRESSION)
    }

    @Test
    fun fixedHuffman_shortRepetitiveInput() {
        // Small inputs stay under Deflater's dynamic-block threshold, producing fixed-Huffman blocks.
        assertRoundTrip("ababababab".encodeToByteArray())
    }

    @Test
    fun dynamicHuffman_largeTextInput() {
        val text = buildString {
            repeat(2000) { append("hello world, this is a test of the ods xml content ") }
        }
        assertRoundTrip(text.encodeToByteArray())
    }

    @Test
    fun dynamicHuffman_xmlLikeContent() {
        val xml = buildString {
            append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>")
            append("<office:document-content xmlns:office=\"urn:oasis:names:tc:opendocument:xmlns:office:1.0\">")
            repeat(500) { i ->
                append("<table:table-row><table:table-cell office:value-type=\"string\">")
                append("Row $i value ${i * 3.14}")
                append("</table:table-cell></table:table-row>")
            }
            append("</office:document-content>")
        }
        assertRoundTrip(xml.encodeToByteArray())
    }

    @Test
    fun highlyRepetitiveInput_exercisesLongBackReferences() {
        // Long runs of a single byte force length/distance combinations near
        // the DEFLATE maximums (length up to 258), including overlapping copies.
        assertRoundTrip(ByteArray(50_000) { 'x'.code.toByte() })
    }

    @Test
    fun binaryRandomInput_lowCompressibility() {
        val random = Random(42)
        assertRoundTrip(random.nextBytes(20_000))
    }

    @Test
    fun mixedContent_multipleBlocks() {
        // Large enough and varied enough that Deflater emits several blocks.
        val builder = StringBuilder()
        val random = Random(7)
        repeat(30_000) {
            builder.append(if (random.nextInt(10) == 0) random.nextInt(256).toChar() else 'a' + random.nextInt(5))
        }
        assertRoundTrip(builder.toString().encodeToByteArray())
    }

    @Test
    fun allCompressionLevels() {
        val input = "The quick brown fox jumps over the lazy dog. ".repeat(300).encodeToByteArray()
        for (level in Deflater.NO_COMPRESSION..Deflater.BEST_COMPRESSION) {
            assertRoundTrip(input, level)
        }
    }
}
