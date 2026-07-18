package com.hhldiniz.praondefoiomeudinheiro.data.local

import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CsvUriHolderTest {

    @Test
    fun defaultIsEmpty() {
        CsvUriHolder.uris = emptyList()
        assertEquals(0, CsvUriHolder.uris.size)
    }

    @Test
    fun storesUris() {
        val uris = listOf(
            Uri.parse("content://a"),
            Uri.parse("content://b")
        )
        CsvUriHolder.uris = uris
        assertEquals(uris, CsvUriHolder.uris)
    }

    @Test
    fun overwritesPreviousValue() {
        CsvUriHolder.uris = listOf(Uri.parse("content://a"))
        CsvUriHolder.uris = listOf(Uri.parse("content://b"), Uri.parse("content://c"))
        assertEquals(2, CsvUriHolder.uris.size)
        assertEquals("content://c", CsvUriHolder.uris[1].toString())
    }
}
