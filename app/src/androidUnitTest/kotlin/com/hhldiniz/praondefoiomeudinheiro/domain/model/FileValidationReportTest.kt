package com.hhldiniz.praondefoiomeudinheiro.domain.model

import com.hhldiniz.praondefoiomeudinheiro.domain.file.InMemoryPlatformFile
import com.hhldiniz.praondefoiomeudinheiro.resources.Res
import com.hhldiniz.praondefoiomeudinheiro.resources.error_unsupported_format
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Files are addressed through [com.hhldiniz.praondefoiomeudinheiro.domain.file.PlatformFile]
 * instead of `android.net.Uri`, so this no longer needs a device and runs as a
 * plain JVM test.
 */
class FileValidationReportTest {

    private val validFile = InMemoryPlatformFile("a.csv")
    private val invalidFile = InMemoryPlatformFile("b.txt")
    private val unsupported = UiText.Localized(Res.string.error_unsupported_format)

    @Test
    fun reportWithValidFiles_hasValidFilesTrue() {
        val report = FileValidationReport(
            validFiles = listOf(
                ValidSpreadsheetFile(
                    name = "a.csv",
                    file = validFile,
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
                InvalidSpreadsheetFile(name = "b.txt", file = invalidFile, reason = unsupported)
            )
        )
        assertFalse(report.hasValidFiles)
        assertTrue(report.hasInvalidFiles)
    }

    @Test
    fun reportWithMixedFiles_hasValidFilesTrue() {
        val report = FileValidationReport(
            validFiles = listOf(
                ValidSpreadsheetFile(name = "a.csv", file = validFile, headerColumns = emptyList())
            ),
            invalidFiles = listOf(
                InvalidSpreadsheetFile(name = "b.txt", file = invalidFile, reason = unsupported)
            )
        )
        assertTrue(report.hasValidFiles)
        assertTrue(report.hasInvalidFiles)
    }

    @Test
    fun validSpreadsheetFileHoldsFields() {
        val valid = ValidSpreadsheetFile(
            name = "a.csv",
            file = validFile,
            headerColumns = listOf("a", "b"),
            headerRowIndex = 3
        )
        assertEquals("a.csv", valid.name)
        assertEquals(validFile, valid.file)
        assertEquals(listOf("a", "b"), valid.headerColumns)
        assertEquals(3, valid.headerRowIndex)
    }

    @Test
    fun invalidSpreadsheetFileHoldsReason() {
        val invalid = InvalidSpreadsheetFile(
            name = "b.txt",
            file = invalidFile,
            reason = UiText.Raw("nope"),
        )
        assertEquals(UiText.Raw("nope"), invalid.reason)
        assertEquals("b.txt", invalid.name)
    }

    @Test
    fun emptyReport_hasValidFilesFalse() {
        val report = FileValidationReport(emptyList(), emptyList())
        assertFalse(report.hasValidFiles)
        assertFalse(report.hasInvalidFiles)
    }
}
