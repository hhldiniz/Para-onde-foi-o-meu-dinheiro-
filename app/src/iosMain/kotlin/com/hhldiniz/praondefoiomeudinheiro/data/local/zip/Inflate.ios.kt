package com.hhldiniz.praondefoiomeudinheiro.data.local.zip

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.alloc
import kotlinx.cinterop.convert
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.sizeOf
import kotlinx.cinterop.usePinned
import platform.zlib.Z_FINISH
import platform.zlib.Z_OK
import platform.zlib.Z_STREAM_END
import platform.zlib.ZLIB_VERSION
import platform.zlib.inflate
import platform.zlib.inflateEnd
import platform.zlib.inflateInit2_
import platform.zlib.z_stream

/**
 * Raw DEFLATE decompression via the zlib that ships with the system.
 * `windowBits = -15` is zlib's way of saying "no zlib/gzip header", which is
 * exactly how ZIP stores entry data.
 */
@OptIn(ExperimentalForeignApi::class)
actual fun inflateRaw(deflated: ByteArray, expectedSize: Int): ByteArray {
    if (deflated.isEmpty()) return ByteArray(0)

    var capacity = if (expectedSize > 0) expectedSize else deflated.size * 4
    while (true) {
        val output = ByteArray(capacity)
        var produced = 0
        var completed = false

        memScoped {
            val stream = alloc<z_stream>()
            check(inflateInit2_(stream.ptr, -15, ZLIB_VERSION, sizeOf<z_stream>().toInt()) == Z_OK) {
                "zlib could not be initialised"
            }
            try {
                deflated.usePinned { input ->
                    output.usePinned { out ->
                        stream.next_in = input.addressOf(0).reinterpret()
                        stream.avail_in = deflated.size.convert()
                        stream.next_out = out.addressOf(0).reinterpret()
                        stream.avail_out = output.size.convert()
                        val result = inflate(stream.ptr, Z_FINISH)
                        produced = output.size - stream.avail_out.toInt()
                        completed = result == Z_STREAM_END
                    }
                }
            } finally {
                inflateEnd(stream.ptr)
            }
        }

        if (completed) return output.copyOf(produced)
        // The output buffer was too small (only possible when the archive did
        // not record an uncompressed size); retry with more room.
        capacity *= 2
    }
}
