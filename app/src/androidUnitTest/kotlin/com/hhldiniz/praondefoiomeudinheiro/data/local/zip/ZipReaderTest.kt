package com.hhldiniz.praondefoiomeudinheiro.data.local.zip

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Covers the common-code ZIP reader that replaced `java.util.zip` in the ODS
 * import path. Archives are produced with `java.util.zip` so the test asserts
 * against real-world output rather than the reader's own idea of the format.
 */
class ZipReaderTest {

    private fun zipOf(entries: Map<String, ByteArray>, stored: Boolean = false): ByteArray {
        val out = ByteArrayOutputStream()
        ZipOutputStream(out).use { zip ->
            if (stored) zip.setMethod(ZipOutputStream.STORED)
            entries.forEach { (name, bytes) ->
                val entry = ZipEntry(name)
                if (stored) {
                    entry.size = bytes.size.toLong()
                    entry.compressedSize = bytes.size.toLong()
                    entry.crc = java.util.zip.CRC32().apply { update(bytes) }.value
                }
                zip.putNextEntry(entry)
                zip.write(bytes)
                zip.closeEntry()
            }
        }
        return out.toByteArray()
    }

    @Test
    fun readsDeflatedEntry() {
        val content = "hello ".repeat(500)
        val archive = zipOf(mapOf("content.xml" to content.encodeToByteArray()))
        assertEquals(content, ZipReader.readEntry(archive, "content.xml")?.decodeToString())
    }

    @Test
    fun readsStoredEntry() {
        val content = "application/vnd.oasis.opendocument.spreadsheet"
        val archive = zipOf(mapOf("mimetype" to content.encodeToByteArray()), stored = true)
        assertEquals(content, ZipReader.readEntry(archive, "mimetype")?.decodeToString())
    }

    @Test
    fun picksTheRequestedEntryAmongSeveral() {
        val archive = zipOf(
            mapOf(
                "mimetype" to "x".encodeToByteArray(),
                "content.xml" to "<doc/>".encodeToByteArray(),
                "styles.xml" to "<styles/>".encodeToByteArray(),
            )
        )
        assertEquals("<doc/>", ZipReader.readEntry(archive, "content.xml")?.decodeToString())
        assertEquals("<styles/>", ZipReader.readEntry(archive, "styles.xml")?.decodeToString())
    }

    @Test
    fun missingEntryReturnsNull() {
        val archive = zipOf(mapOf("mimetype" to "x".encodeToByteArray()))
        assertNull(ZipReader.readEntry(archive, "content.xml"))
    }

    @Test
    fun nonZipInputReturnsNull() {
        assertNull(ZipReader.readEntry("not a zip at all".encodeToByteArray(), "content.xml"))
    }

    @Test
    fun emptyEntryReadsAsEmpty() {
        val archive = zipOf(mapOf("content.xml" to ByteArray(0)))
        assertEquals(0, ZipReader.readEntry(archive, "content.xml")?.size)
    }
}
