package com.hhldiniz.praondefoiomeudinheiro.data.local

import android.net.Uri

/**
 * Holds the list of content URIs selected by the user during the landing
 * flow, so they can be consumed later by [HomeViewModel].
 */
object CsvUriHolder {
    var uris: List<Uri> = emptyList()
}

