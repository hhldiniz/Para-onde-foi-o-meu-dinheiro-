package com.hhldiniz.praondefoiomeudinheiro.data.local.zip

/**
 * Minimal read-only ZIP container reader, enough to pull a single named entry
 * out of an `.ods` file. `java.util.zip` is JVM-only, so the container format
 * is walked here in common code and only the DEFLATE step is delegated to the
 * platform (see [inflateRaw]).
 *
 * ZIP64 archives are not supported; ODS spreadsheets never reach 4 GB.
 */
object ZipReader {

    private const val END_OF_CENTRAL_DIRECTORY = 0x06054b50
    private const val CENTRAL_FILE_HEADER = 0x02014b50
    private const val LOCAL_FILE_HEADER = 0x04034b50

    private const val METHOD_STORED = 0
    private const val METHOD_DEFLATED = 8

    /** Returns the decompressed bytes of [entryName], or null when the archive has no such entry. */
    fun readEntry(archive: ByteArray, entryName: String): ByteArray? {
        val eocd = findEndOfCentralDirectory(archive) ?: return null
        val entryCount = readU16(archive, eocd + 10)
        var offset = readU32(archive, eocd + 16).toInt()

        repeat(entryCount) {
            if (offset < 0 || offset + 46 > archive.size) return null
            if (readU32(archive, offset).toInt() != CENTRAL_FILE_HEADER) return null

            val method = readU16(archive, offset + 10)
            val compressedSize = readU32(archive, offset + 20).toInt()
            val uncompressedSize = readU32(archive, offset + 24).toInt()
            val nameLength = readU16(archive, offset + 28)
            val extraLength = readU16(archive, offset + 30)
            val commentLength = readU16(archive, offset + 32)
            val localHeaderOffset = readU32(archive, offset + 42).toInt()

            val name = archive.decodeToString(offset + 46, offset + 46 + nameLength)
            if (name == entryName) {
                return readLocalEntry(archive, localHeaderOffset, method, compressedSize, uncompressedSize)
            }

            offset += 46 + nameLength + extraLength + commentLength
        }
        return null
    }

    private fun readLocalEntry(
        archive: ByteArray,
        localHeaderOffset: Int,
        method: Int,
        compressedSize: Int,
        uncompressedSize: Int,
    ): ByteArray? {
        if (localHeaderOffset < 0 || localHeaderOffset + 30 > archive.size) return null
        if (readU32(archive, localHeaderOffset).toInt() != LOCAL_FILE_HEADER) return null

        val nameLength = readU16(archive, localHeaderOffset + 26)
        val extraLength = readU16(archive, localHeaderOffset + 28)
        val dataStart = localHeaderOffset + 30 + nameLength + extraLength
        if (dataStart + compressedSize > archive.size) return null

        val data = archive.copyOfRange(dataStart, dataStart + compressedSize)
        return when (method) {
            METHOD_STORED -> data
            METHOD_DEFLATED -> inflateRaw(data, uncompressedSize)
            else -> null
        }
    }

    /**
     * Scans backwards for the end-of-central-directory record. The trailing
     * comment can be up to 64 KiB, so that is how far back the search goes.
     */
    private fun findEndOfCentralDirectory(archive: ByteArray): Int? {
        val minOffset = maxOf(0, archive.size - (0xFFFF + 22))
        var offset = archive.size - 22
        while (offset >= minOffset) {
            if (readU32(archive, offset).toInt() == END_OF_CENTRAL_DIRECTORY) return offset
            offset--
        }
        return null
    }

    private fun readU16(bytes: ByteArray, offset: Int): Int =
        (bytes[offset].toInt() and 0xFF) or ((bytes[offset + 1].toInt() and 0xFF) shl 8)

    private fun readU32(bytes: ByteArray, offset: Int): Long =
        (bytes[offset].toLong() and 0xFF) or
            ((bytes[offset + 1].toLong() and 0xFF) shl 8) or
            ((bytes[offset + 2].toLong() and 0xFF) shl 16) or
            ((bytes[offset + 3].toLong() and 0xFF) shl 24)
}

/**
 * Decompresses raw (headerless) DEFLATE data. [expectedSize] is the entry's
 * uncompressed size as recorded in the archive, which lets platforms allocate
 * the output buffer up front.
 */
expect fun inflateRaw(deflated: ByteArray, expectedSize: Int): ByteArray
