package com.hhldiniz.praondefoiomeudinheiro.domain.model

import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FileValidationReportTest {

    private val validUri = Uri.parse("file:///a.csv")
    private val invalidUri = Uri.parse("file:///b.txt")

    @Test
    fun reportWithValidFiles_hasValidFilesTrue() {
        val report = FileValidationReport(
            validFiles = listOf(
                ValidSpreadsheetFile(
                    name = "a.csv",
                    uri = validUri,
                    headerColumns = listOf("date", "amount")
                )
            ),
            invalidFiles = emptyList()
        )
        assertTrue(report.hasValidFiles)
        assertFalse(report.hasInvalidFiles)
    }

    @Test
    fun reportWithOnlyInvalidFiles_hasValidFilesFalse() {
        val report = FileValidationReport(
            validFiles = emptyList(),
            invalidFiles = listOf(
                InvalidSpreadsheetFile(name = "b.txt", uri = invalidUri, reason = "bad")
            )
        )
        assertFalse(report.hasValidFiles)
        assertTrue(report.hasInvalidFiles)
    }

    @Test
    fun reportWithMixedFiles_hasValidFilesTrue() {
        val report = FileValidationReport(
            validFiles = listOf(
                ValidSpreadsheetFile(name = "a.csv", uri = validUri, headerColumns = emptyList())
            ),
            invalidFiles = listOf(
                InvalidSpreadsheetFile(name = "b.txt", uri = invalidUri, reason = "bad")
            )
        )
        assertTrue(report.hasValidFiles)
        assertTrue(report.hasInvalidFiles)
    }

    @Test
    fun validSpreadsheetFileHoldsFields() {
        val valid = ValidSpreadsheetFile(
            name = "a.csv",
            uri = validUri,
            headerColumns = listOf("a", "b"),
            headerRowIndex = 3
        )
        assertEquals("a.csv", valid.name)
        assertEquals(validUri, valid.uri)
        assertEquals(listOf("a", "b"), valid.headerColumns)
        assertEquals(3, valid.headerRowIndex)
    }

    @Test
    fun invalidSpreadsheetFileHoldsReason() {
        val invalid = InvalidSpreadsheetFile(name = "b.txt", uri = invalidUri, reason = "nope")
        assertEquals("nope", invalid.reason)
        assertEquals("b.txt", invalid.name)
    }

    @Test
    fun emptyReport_hasValidFilesFalse() {
        val report = FileValidationReport(emptyList(), emptyList())
        assertFalse(report.hasValidFiles)
        assertFalse(report.hasInvalidFiles)
    }
}
