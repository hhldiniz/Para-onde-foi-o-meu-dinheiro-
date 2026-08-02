package com.hhldiniz.praondefoiomeudinheiro.data.vision

import com.hhldiniz.praondefoiomeudinheiro.domain.file.InMemoryPlatformFile
import com.hhldiniz.praondefoiomeudinheiro.domain.vision.BoundingBox
import com.hhldiniz.praondefoiomeudinheiro.domain.vision.RecognizedDocument
import com.hhldiniz.praondefoiomeudinheiro.domain.vision.RecognizedWord
import com.hhldiniz.praondefoiomeudinheiro.domain.vision.SmartImportSource
import com.hhldiniz.praondefoiomeudinheiro.domain.vision.TransactionField
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * End-to-end tests for the automatic import, with a canned recognizer standing
 * in for ML Kit / Vision / Tesseract.js so the whole pipeline can be exercised
 * on the JVM.
 */
class SmartImportAnalyzerTest {

    private val lineHeight = 0.03f

    private fun word(text: String, left: Float, top: Float, right: Float) =
        RecognizedWord(text, BoundingBox(left, top, right, top + lineHeight), confidence = 0.9f)

    /** The words a recognizer would return for a photographed three-column statement. */
    private val photographedStatement = RecognizedDocument(
        listOf(
            word("Data", 0.05f, 0.10f, 0.13f),
            word("Descrição", 0.30f, 0.10f, 0.44f),
            word("Valor", 0.75f, 0.10f, 0.84f),

            word("15/03/2024", 0.05f, 0.20f, 0.20f),
            word("Mercado", 0.30f, 0.20f, 0.40f),
            word("Bom", 0.405f, 0.20f, 0.44f),
            word("Preço", 0.445f, 0.20f, 0.49f),
            word("R$", 0.75f, 0.20f, 0.79f),
            word("120,50", 0.795f, 0.20f, 0.88f),

            word("16/03/2024", 0.05f, 0.30f, 0.20f),
            word("Uber", 0.30f, 0.30f, 0.36f),
            word("R$", 0.79f, 0.30f, 0.83f),
            word("32,00", 0.835f, 0.30f, 0.88f),
        )
    )

    private fun analyzer(document: RecognizedDocument = photographedStatement) =
        SmartImportAnalyzer { document }

    @Test
    fun analyze_photo_readsTransactionsThroughTheRecognizer() = runBlocking {
        val file = InMemoryPlatformFile("extrato.jpg", byteArrayOf(1, 2, 3))

        val analysis = analyzer().analyze(file).getOrThrow()

        assertEquals(SmartImportSource.IMAGE, analysis.source)
        assertEquals(2, analysis.transactions.size)
        assertEquals("Mercado Bom Preço", analysis.transactions[0].description)
        assertEquals(120.50, analysis.transactions[0].amount, 0.001)
        assertEquals(0, analysis.columnOf(TransactionField.DATE))
        assertEquals(2, analysis.columnOf(TransactionField.AMOUNT))
    }

    @Test
    fun analyze_photo_confidenceIsCappedByTheRecognizer() = runBlocking {
        val analysis = analyzer().analyze(InMemoryPlatformFile("extrato.png")).getOrThrow()

        // The recognizer reported 0.9 per word, so no row may claim more.
        assertTrue(analysis.transactions.all { it.confidence <= 0.9f })
    }

    @Test
    fun analyze_csv_takesTheSameRouteWithoutTheRecognizer() = runBlocking {
        val csv = """
            Data,Descrição,Categoria,Valor
            15/03/2024,Mercado Bom Preço,Alimentacao,120.50
            16/03/2024,Uber para o trabalho,Transporte,32.00
        """.trimIndent().toByteArray()

        val analysis = SmartImportAnalyzer { error("must not be called for a spreadsheet") }
            .analyze(InMemoryPlatformFile("gastos.csv", csv))
            .getOrThrow()

        assertEquals(SmartImportSource.CSV, analysis.source)
        assertEquals(2, analysis.transactions.size)
        assertEquals("Alimentacao", analysis.transactions[0].category)
    }

    @Test
    fun analyze_csvWithAnUnexpectedLayout_stillImports() = runBlocking {
        // No header, amount first, an extra column the app knows nothing
        // about: the direct importer would reject this file.
        val csv = """
            1,120.50,15/03/2024,Mercado Bom Preço
            2,32.00,16/03/2024,Uber para o trabalho
            3,45.00,17/03/2024,Cinema com amigos
        """.trimIndent().toByteArray()

        val analysis = SmartImportAnalyzer { error("must not be called for a spreadsheet") }
            .analyze(InMemoryPlatformFile("desconhecido.csv", csv))
            .getOrThrow()

        assertEquals(3, analysis.transactions.size)
        assertEquals(2, analysis.columnOf(TransactionField.DATE))
        assertEquals(1, analysis.columnOf(TransactionField.AMOUNT))
    }

    @Test
    fun analyze_unrecognizableImage_failsInsteadOfThrowing() = runBlocking {
        val failing = SmartImportAnalyzer { throw IllegalArgumentException("corrupt image") }

        val result = failing.analyze(InMemoryPlatformFile("foto.png", byteArrayOf(0)))

        assertTrue(result.isFailure)
        assertEquals("corrupt image", result.exceptionOrNull()?.message)
    }

    @Test
    fun analyze_pageWithoutTransactions_returnsAnEmptyAnalysis() = runBlocking {
        val shoppingList = RecognizedDocument(
            listOf(
                word("Arroz", 0.05f, 0.10f, 0.20f),
                word("2", 0.60f, 0.10f, 0.63f),
                word("Feijão", 0.05f, 0.20f, 0.20f),
                word("1", 0.60f, 0.20f, 0.63f),
            )
        )

        val analysis = analyzer(shoppingList).analyze(InMemoryPlatformFile("lista.png")).getOrThrow()

        assertTrue(analysis.isEmpty)
        assertFalse(analysis.confidence > 0f)
    }

    @Test
    fun sourceOf_readsExtensionsFirstAndMimeTypesSecond() {
        val analyzer = analyzer()

        assertEquals(SmartImportSource.IMAGE, analyzer.sourceOf("foto.JPG"))
        assertEquals(SmartImportSource.IMAGE, analyzer.sourceOf("captura.png"))
        assertEquals(SmartImportSource.PDF, analyzer.sourceOf("extrato.pdf"))
        assertEquals(SmartImportSource.ODS, analyzer.sourceOf("planilha.ods"))
        assertEquals(SmartImportSource.CSV, analyzer.sourceOf("gastos.csv"))
        assertEquals(SmartImportSource.IMAGE, analyzer.sourceOf("document", "image/heic"))
        assertEquals(SmartImportSource.PDF, analyzer.sourceOf("document", "application/pdf"))
    }
}
