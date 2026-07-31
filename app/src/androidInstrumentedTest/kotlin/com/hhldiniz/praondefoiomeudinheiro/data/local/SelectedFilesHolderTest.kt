package com.hhldiniz.praondefoiomeudinheiro.data.local

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.hhldiniz.praondefoiomeudinheiro.domain.file.InMemoryPlatformFile
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SelectedFilesHolderTest {

    @Test
    fun defaultIsEmpty() {
        SelectedFilesHolder.files = emptyList()
        assertEquals(0, SelectedFilesHolder.files.size)
    }

    @Test
    fun storesFiles() {
        val files = listOf(
            InMemoryPlatformFile("a.csv"),
            InMemoryPlatformFile("b.csv"),
        )
        SelectedFilesHolder.files = files
        assertEquals(files, SelectedFilesHolder.files)
    }

    @Test
    fun overwritesPreviousValue() {
        SelectedFilesHolder.files = listOf(InMemoryPlatformFile("a.csv"))
        SelectedFilesHolder.files = listOf(
            InMemoryPlatformFile("b.csv"),
            InMemoryPlatformFile("c.csv"),
        )
        assertEquals(2, SelectedFilesHolder.files.size)
        assertEquals("c.csv", SelectedFilesHolder.files[1].name)
    }
}
