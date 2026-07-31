package com.hhldiniz.praondefoiomeudinheiro.data.local.zip

// The browser's own decompression (DecompressionStream) is Promise/stream-based
// and doesn't fit this function's synchronous signature, so wasmJs gets the
// pure-Kotlin decoder in RawInflate.kt instead (also used by a JVM-runnable
// cross-check test against java.util.zip.Deflater — see RawInflateTest).
actual fun inflateRaw(deflated: ByteArray, expectedSize: Int): ByteArray =
    RawInflate.inflate(deflated, expectedSize)
