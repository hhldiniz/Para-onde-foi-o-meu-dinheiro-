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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
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

    /** The words a recognizer would return for a photographed supermarket receipt. */
    private val receiptPhoto = RecognizedDocument(
        listOf(
            word("Mercado", 0.20f, 0.04f, 0.38f),
            word("Bom", 0.39f, 0.04f, 0.47f),
            word("Preço", 0.48f, 0.04f, 0.60f),

            word("CNPJ", 0.20f, 0.09f, 0.30f),
            word("12.345.678/0001-90", 0.31f, 0.09f, 0.62f),

            word("Emissão:", 0.20f, 0.14f, 0.34f),
            word("15/03/2024", 0.35f, 0.14f, 0.52f),

            word("001", 0.05f, 0.24f, 0.10f),
            word("ARROZ", 0.12f, 0.24f, 0.25f),
            word("TIPO", 0.26f, 0.24f, 0.34f),
            word("1", 0.35f, 0.24f, 0.37f),
            word("2", 0.55f, 0.24f, 0.57f),
            word("UN", 0.58f, 0.24f, 0.63f),
            word("x", 0.64f, 0.24f, 0.66f),
            word("12,50", 0.67f, 0.24f, 0.77f),
            word("25,00", 0.85f, 0.24f, 0.95f),

            word("002", 0.05f, 0.29f, 0.10f),
            word("FEIJÃO", 0.12f, 0.29f, 0.26f),
            word("PRETO", 0.27f, 0.29f, 0.38f),
            word("20,40", 0.85f, 0.29f, 0.95f),

            word("Qtd.", 0.05f, 0.36f, 0.12f),
            word("total", 0.13f, 0.36f, 0.22f),
            word("de", 0.23f, 0.36f, 0.27f),
            word("itens:", 0.28f, 0.36f, 0.37f),
            word("2", 0.90f, 0.36f, 0.93f),

            word("VALOR", 0.05f, 0.41f, 0.17f),
            word("TOTAL", 0.18f, 0.41f, 0.30f),
            word("R$", 0.31f, 0.41f, 0.36f),
            word("45,40", 0.85f, 0.41f, 0.95f),

            word("Forma", 0.05f, 0.46f, 0.16f),
            word("de", 0.17f, 0.46f, 0.21f),
            word("pagamento:", 0.22f, 0.46f, 0.40f),
            word("Dinheiro", 0.41f, 0.46f, 0.56f),
            word("50,00", 0.85f, 0.46f, 0.95f),

            word("Troco", 0.05f, 0.51f, 0.16f),
            word("4,60", 0.87f, 0.51f, 0.95f),
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
        assertEquals(SmartImportSource.ODS, analyzer.sourceOf("planilha.ods"))
        assertEquals(SmartImportSource.CSV, analyzer.sourceOf("gastos.csv"))
        assertEquals(SmartImportSource.IMAGE, analyzer.sourceOf("document", "image/heic"))
    }

    @Test
    fun sourceOf_pdfIsNotRead() {
        val analyzer = analyzer()

        assertNull(analyzer.sourceOf("extrato.pdf"))
        assertNull(analyzer.sourceOf("EXTRATO.PDF"))
        assertNull(analyzer.sourceOf("document", "application/pdf"))
    }

    @Test
    fun analyze_pdf_failsAndPointsAtTheDirectImport() = runBlocking {
        val result = analyzer().analyze(InMemoryPlatformFile("extrato.pdf", byteArrayOf(1)))

        assertTrue(result.exceptionOrNull() is UnsupportedImportSourceException)
    }

    @Test
    fun analyzeReceipt_readsThePhotoAsOnePurchase() = runBlocking {
        val result = analyzer(receiptPhoto).analyzeReceipt(InMemoryPlatformFile("nota.jpg"))

        val receipt = result.getOrThrow()?.receipt
        assertNotNull(receipt)
        assertEquals("Mercado Bom Preço", receipt!!.merchant)
        assertEquals(45.40, receipt.total, 0.001)
        assertEquals(2, receipt.items.size)
        assertEquals("Alimentacao", receipt.category)
    }

    @Test
    fun analyzeReceipt_rejectsAnythingThatIsNotAPhoto() = runBlocking {
        val result = analyzer().analyzeReceipt(InMemoryPlatformFile("nota.csv", byteArrayOf(1)))

        assertTrue(result.exceptionOrNull() is ReceiptRequiresImageException)
    }

    @Test
    fun analyzeReceipt_photoWithoutAmounts_succeedsWithNothingToReview() = runBlocking {
        val poster = RecognizedDocument(
            listOf(
                word("Feira", 0.05f, 0.10f, 0.20f),
                word("de", 0.21f, 0.10f, 0.25f),
                word("domingo", 0.26f, 0.10f, 0.40f),
            )
        )

        val result = analyzer(poster).analyzeReceipt(InMemoryPlatformFile("cartaz.png"))

        assertTrue(result.isSuccess)
        assertNull(result.getOrThrow())
    }
}
