package com.hhldiniz.praondefoiomeudinheiro.data.local.zip

import java.io.ByteArrayOutputStream
import java.util.zip.Inflater

actual fun inflateRaw(deflated: ByteArray, expectedSize: Int): ByteArray {
    // `nowrap = true` selects raw DEFLATE, which is what ZIP entries store.
    val inflater = Inflater(true)
    try {
        inflater.setInput(deflated)
        val output = ByteArrayOutputStream(if (expectedSize > 0) expectedSize else deflated.size * 4)
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        while (!inflater.finished()) {
            val read = inflater.inflate(buffer)
            if (read == 0 && (inflater.needsInput() || inflater.needsDictionary())) break
            output.write(buffer, 0, read)
        }
        return output.toByteArray()
    } finally {
        inflater.end()
    }
}
