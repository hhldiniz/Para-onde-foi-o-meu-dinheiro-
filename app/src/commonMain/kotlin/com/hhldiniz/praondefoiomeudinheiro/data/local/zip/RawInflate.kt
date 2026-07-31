package com.hhldiniz.praondefoiomeudinheiro.data.local.zip

/**
 * Pure-Kotlin RFC 1951 raw (headerless, `nowrap`) DEFLATE decoder — the same
 * bitstream `java.util.zip.Inflater(true)` and zlib's `inflateInit2_(..., -15, ...)`
 * decode. Lives in commonMain (rather than only under wasmJs, which is the
 * only platform that needs it) so it can be exercised by a JVM unit test
 * cross-checked against `java.util.zip.Deflater`; see `RawInflateTest`.
 */
object RawInflate {

    private val LENGTH_BASE = intArrayOf(
        3, 4, 5, 6, 7, 8, 9, 10, 11, 13, 15, 17, 19, 23, 27, 31,
        35, 43, 51, 59, 67, 83, 99, 115, 131, 163, 195, 227, 258,
    )
    private val LENGTH_EXTRA_BITS = intArrayOf(
        0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 2, 2, 2, 2,
        3, 3, 3, 3, 4, 4, 4, 4, 5, 5, 5, 5, 0,
    )
    private val DIST_BASE = intArrayOf(
        1, 2, 3, 4, 5, 7, 9, 13, 17, 25, 33, 49, 65, 97, 129, 193,
        257, 385, 513, 769, 1025, 1537, 2049, 3073, 4097, 6145, 8193, 12289, 16385, 24577,
    )
    private val DIST_EXTRA_BITS = intArrayOf(
        0, 0, 0, 0, 1, 1, 2, 2, 3, 3, 4, 4, 5, 5, 6, 6,
        7, 7, 8, 8, 9, 9, 10, 10, 11, 11, 12, 12, 13, 13,
    )
    private val CODE_LENGTH_ORDER = intArrayOf(
        16, 17, 18, 0, 8, 7, 9, 6, 10, 5, 11, 4, 12, 3, 13, 2, 14, 1, 15,
    )

    private val FIXED_LITLEN_TREE = HuffmanTree(
        IntArray(288) { symbol ->
            when (symbol) {
                in 0..143 -> 8
                in 144..255 -> 9
                in 256..279 -> 7
                else -> 8
            }
        }
    )
    private val FIXED_DIST_TREE = HuffmanTree(IntArray(30) { 5 })

    /** Decompresses [deflated]. [sizeHint] (the known uncompressed size, if any) just sizes the output buffer up front. */
    fun inflate(deflated: ByteArray, sizeHint: Int = 0): ByteArray {
        if (deflated.isEmpty()) return ByteArray(0)

        val reader = BitReader(deflated)
        val output = GrowableByteBuffer(if (sizeHint > 0) sizeHint else deflated.size * 4)

        while (true) {
            val isFinalBlock = reader.readBits(1) == 1
            when (val blockType = reader.readBits(2)) {
                0 -> inflateStoredBlock(reader, output)
                1 -> inflateCompressedBlock(reader, output, FIXED_LITLEN_TREE, FIXED_DIST_TREE)
                2 -> {
                    val (litLenTree, distTree) = readDynamicTrees(reader)
                    inflateCompressedBlock(reader, output, litLenTree, distTree)
                }
                else -> error("Invalid DEFLATE block type: $blockType")
            }
            if (isFinalBlock) break
        }
        return output.toByteArray()
    }

    private fun inflateStoredBlock(reader: BitReader, output: GrowableByteBuffer) {
        reader.alignToByteBoundary()
        val len = reader.readAlignedByte() or (reader.readAlignedByte() shl 8)
        val nlen = reader.readAlignedByte() or (reader.readAlignedByte() shl 8)
        require((len xor 0xFFFF) and 0xFFFF == nlen) { "Stored block LEN/NLEN mismatch" }
        repeat(len) { output.append(reader.readAlignedByte().toByte()) }
    }

    private fun inflateCompressedBlock(
        reader: BitReader,
        output: GrowableByteBuffer,
        litLenTree: HuffmanTree,
        distTree: HuffmanTree,
    ) {
        while (true) {
            val symbol = litLenTree.decodeSymbol(reader)
            when {
                symbol < 256 -> output.append(symbol.toByte())
                symbol == 256 -> return
                else -> {
                    val lengthIndex = symbol - 257
                    require(lengthIndex < LENGTH_BASE.size) { "Invalid length symbol: $symbol" }
                    val length = LENGTH_BASE[lengthIndex] + reader.readBits(LENGTH_EXTRA_BITS[lengthIndex])
                    val distSymbol = distTree.decodeSymbol(reader)
                    val distance = DIST_BASE[distSymbol] + reader.readBits(DIST_EXTRA_BITS[distSymbol])
                    output.copyFromBack(distance, length)
                }
            }
        }
    }

    private fun readDynamicTrees(reader: BitReader): Pair<HuffmanTree, HuffmanTree> {
        val literalLengthCount = reader.readBits(5) + 257
        val distanceCount = reader.readBits(5) + 1
        val codeLengthCount = reader.readBits(4) + 4

        val codeLengthLengths = IntArray(19)
        for (i in 0 until codeLengthCount) {
            codeLengthLengths[CODE_LENGTH_ORDER[i]] = reader.readBits(3)
        }
        val codeLengthTree = HuffmanTree(codeLengthLengths)

        val allLengths = IntArray(literalLengthCount + distanceCount)
        var i = 0
        while (i < allLengths.size) {
            when (val symbol = codeLengthTree.decodeSymbol(reader)) {
                in 0..15 -> allLengths[i++] = symbol
                16 -> {
                    require(i > 0) { "Repeat code 16 with no previous length" }
                    val previous = allLengths[i - 1]
                    val times = reader.readBits(2) + 3
                    repeat(times) { allLengths[i++] = previous }
                }
                17 -> {
                    val times = reader.readBits(3) + 3
                    repeat(times) { allLengths[i++] = 0 }
                }
                18 -> {
                    val times = reader.readBits(7) + 11
                    repeat(times) { allLengths[i++] = 0 }
                }
                else -> error("Invalid code length symbol: $symbol")
            }
        }

        val litLenTree = HuffmanTree(allLengths.copyOfRange(0, literalLengthCount))
        val distTree = HuffmanTree(allLengths.copyOfRange(literalLengthCount, allLengths.size))
        return litLenTree to distTree
    }
}

/** Reads bits from a byte array in DEFLATE's bit order (least-significant bit of each byte first). */
private class BitReader(private val data: ByteArray) {
    private var byteIndex = 0
    private var bitIndex = 0

    /** Reads [count] bits, least-significant bit first — the packing DEFLATE uses for everything except Huffman codes. */
    fun readBits(count: Int): Int {
        var value = 0
        for (i in 0 until count) {
            value = value or (readBit() shl i)
        }
        return value
    }

    fun readBit(): Int {
        check(byteIndex < data.size) { "Unexpected end of DEFLATE stream" }
        val bit = (data[byteIndex].toInt() ushr bitIndex) and 1
        bitIndex++
        if (bitIndex == 8) {
            bitIndex = 0
            byteIndex++
        }
        return bit
    }

    fun alignToByteBoundary() {
        if (bitIndex != 0) {
            bitIndex = 0
            byteIndex++
        }
    }

    fun readAlignedByte(): Int {
        check(byteIndex < data.size) { "Unexpected end of DEFLATE stream" }
        return (data[byteIndex++].toInt() and 0xFF)
    }
}

/** Canonical Huffman decode table built from a per-symbol code-length array, per RFC 1951 §3.2.2. */
private class HuffmanTree(codeLengths: IntArray) {
    private val maxBits = codeLengths.maxOrNull() ?: 0

    // codesByLength[len][code] = symbol, keyed by the code value for that bit length.
    private val codesByLength: Array<HashMap<Int, Int>> = Array(maxBits + 1) { HashMap() }

    init {
        val countPerLength = IntArray(maxBits + 1)
        for (len in codeLengths) if (len > 0) countPerLength[len]++

        val nextCodePerLength = IntArray(maxBits + 1)
        var code = 0
        for (len in 1..maxBits) {
            code = (code + countPerLength[len - 1]) shl 1
            nextCodePerLength[len] = code
        }

        for (symbol in codeLengths.indices) {
            val len = codeLengths[symbol]
            if (len > 0) {
                codesByLength[len][nextCodePerLength[len]] = symbol
                nextCodePerLength[len]++
            }
        }
    }

    /** Huffman codes are packed most-significant-bit first, unlike every other DEFLATE field. */
    fun decodeSymbol(reader: BitReader): Int {
        var code = 0
        for (len in 1..maxBits) {
            code = (code shl 1) or reader.readBit()
            codesByLength[len][code]?.let { return it }
        }
        error("Invalid Huffman code")
    }
}

/** Minimal growable byte buffer supporting DEFLATE's overlapping back-reference copies. */
private class GrowableByteBuffer(initialCapacity: Int) {
    private var buffer = ByteArray(maxOf(initialCapacity, 64))
    var size = 0
        private set

    private fun ensureCapacity(extra: Int) {
        if (size + extra > buffer.size) {
            var newCapacity = buffer.size * 2
            while (newCapacity < size + extra) newCapacity *= 2
            buffer = buffer.copyOf(newCapacity)
        }
    }

    fun append(byte: Byte) {
        ensureCapacity(1)
        buffer[size++] = byte
    }

    /** Copies [length] bytes from [distance] bytes back in the output, one byte at a time (source and destination can overlap). */
    fun copyFromBack(distance: Int, length: Int) {
        ensureCapacity(length)
        var sourceIndex = size - distance
        require(sourceIndex >= 0) { "Invalid DEFLATE back-reference distance: $distance" }
        repeat(length) {
            buffer[size] = buffer[sourceIndex]
            size++
            sourceIndex++
        }
    }

    fun toByteArray(): ByteArray = buffer.copyOf(size)
}
