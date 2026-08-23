package com.hhldiniz.praondefoiomeudinheiro.data.vision

import com.hhldiniz.praondefoiomeudinheiro.domain.vision.BoundingBox
import com.hhldiniz.praondefoiomeudinheiro.domain.vision.RecognizedDocument
import com.hhldiniz.praondefoiomeudinheiro.domain.vision.RecognizedWord
import com.hhldiniz.praondefoiomeudinheiro.util.formatDayMonthYear
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The receipt reader, driven with hand-placed words the way a recognizer would
 * report them for a photographed receipt.
 *
 * This is the place to add a case when a real receipt is misread: each fixture
 * is one printed layout the reader has to survive — the Brazilian NFC-e coupon
 * with its item codes and "Qtd. total de itens" line, a receipt with no "total"
 * label at all, one that prints the total's value on the line below its label,
 * and one whose service fee has to end up among the items for them to add up.
 */
class ReceiptAnalyzerTest {

    private val lineHeight = 0.03f

    /**
     * Lays a line out as a recognizer would see it: each cell starts at the
     * given fraction of the page width and its words sit next to each other,
     * close enough for [DocumentLayoutAnalyzer] to glue them into one cell.
     */
    private fun line(top: Float, vararg cells: Pair<Float, String>): List<RecognizedWord> =
        cells.flatMap { (start, text) ->
            var x = start
            text.split(" ").map { token ->
                val width = 0.018f * token.length
                val word = RecognizedWord(
                    text = token,
                    box = BoundingBox(x, top, x + width, top + lineHeight),
                    confidence = 0.9f,
                )
                x += width + 0.008f
                word
            }
        }

    /** A supermarket coupon: item codes, a `2 UN x 12,50` tail, payment and change. */
    private val supermarket = RecognizedDocument(
        line(0.04f, 0.20f to "Mercado Bom Preço") +
            line(0.09f, 0.20f to "CNPJ 12.345.678/0001-90") +
            line(0.14f, 0.20f to "Emissão: 15/03/2024 14:32") +
            line(0.24f, 0.05f to "001 ARROZ TIPO 1", 0.55f to "2 UN x 12,50", 0.85f to "25,00") +
            line(0.29f, 0.05f to "002 FEIJÃO PRETO", 0.85f to "20,40") +
            line(0.36f, 0.05f to "Qtd. total de itens:", 0.90f to "2") +
            line(0.41f, 0.05f to "VALOR TOTAL R$", 0.85f to "45,40") +
            line(0.46f, 0.05f to "Forma de pagamento: Dinheiro", 0.85f to "50,00") +
            line(0.51f, 0.05f to "Troco", 0.87f to "4,60")
    )

    /** A receipt that never writes "total": the amount due is just the last line. */
    private val unlabelledTotal = RecognizedDocument(
        line(0.05f, 0.20f to "Drogaria Saude Sempre") +
            line(0.12f, 0.05f to "Dipirona 500mg", 0.85f to "12,90") +
            line(0.18f, 0.05f to "Protetor solar", 0.85f to "48,50") +
            line(0.30f, 0.05f to "R$", 0.85f to "61,40")
    )

    /** An English receipt with a subtotal and the total's value on its own line. */
    private val totalOnNextLine = RecognizedDocument(
        line(0.05f, 0.20f to "Corner Coffee Shop") +
            line(0.12f, 0.05f to "2 x Espresso", 0.85f to "7.00") +
            line(0.17f, 0.05f to "Blueberry muffin", 0.85f to "4.25") +
            line(0.24f, 0.05f to "SUBTOTAL", 0.85f to "11.25") +
            line(0.29f, 0.05f to "TOTAL") +
            line(0.34f, 0.85f to "11.25")
    )

    @Test
    fun analyze_supermarketCoupon_readsMerchantDateAndTotal() {
        val receipt = ReceiptAnalyzer.analyze(supermarket)

        assertNotNull(receipt)
        assertEquals("Mercado Bom Preço", receipt!!.merchant)
        assertEquals("12.345.678/0001-90", receipt.documentId)
        assertEquals("15/03/2024", formatDayMonthYear(receipt.dateMillis))
        assertEquals(45.40, receipt.total, 0.001)
        assertTrue(receipt.totalWasLabelled)
    }

    @Test
    fun analyze_itemCountLine_isNotMistakenForTheTotal() {
        // "Qtd. total de itens: 2" carries the word "total" and a number.
        val receipt = ReceiptAnalyzer.analyze(supermarket)!!

        assertEquals(45.40, receipt.total, 0.001)
    }

    @Test
    fun analyze_paymentAndChange_areNeitherTheTotalNorItems() {
        val receipt = ReceiptAnalyzer.analyze(supermarket)!!

        assertEquals(2, receipt.items.size)
        assertFalse(receipt.items.any { it.amount == 50.00 || it.amount == 4.60 })
    }

    @Test
    fun analyze_itemLine_keepsItsNameAndTakesTheQuantityFromTheUnitPriceTail() {
        val receipt = ReceiptAnalyzer.analyze(supermarket)!!

        val arroz = receipt.items[0]
        // The trailing "1" belongs to the product's name; the quantity is the
        // "2" in front of the "2 UN x 12,50" tail.
        assertEquals("ARROZ TIPO 1", arroz.description)
        assertEquals(25.00, arroz.amount, 0.001)
        assertEquals(2.0, arroz.quantity!!, 0.001)

        val feijao = receipt.items[1]
        assertEquals("FEIJÃO PRETO", feijao.description)
        assertNull(feijao.quantity)
    }

    @Test
    fun analyze_supermarketCoupon_itemsAddUpToTheTotal() {
        val receipt = ReceiptAnalyzer.analyze(supermarket)!!

        assertEquals(45.40, receipt.itemsTotal, 0.001)
        assertTrue(receipt.itemsMatchTotal)
    }

    @Test
    fun analyze_supermarketCoupon_guessesTheCategoryFromTheMerchant() {
        assertEquals("Alimentacao", ReceiptAnalyzer.analyze(supermarket)!!.category)
    }

    @Test
    fun analyze_withoutATotalLabel_fallsBackToTheLargestValueAndSaysSo() {
        val receipt = ReceiptAnalyzer.analyze(unlabelledTotal)!!

        assertEquals(61.40, receipt.total, 0.001)
        assertFalse(receipt.totalWasLabelled)
        // Both penalties apply: the total was guessed and there is no date.
        assertTrue(receipt.confidence < 0.9f)
        assertEquals("Saude", receipt.category)
    }

    @Test
    fun analyze_withoutADate_reportsNoRawDateRatherThanInventingOne() {
        val receipt = ReceiptAnalyzer.analyze(unlabelledTotal)!!

        assertEquals("", receipt.rawDate)
        assertTrue(receipt.dateMillis > 0L)
    }

    @Test
    fun analyze_totalPrintedBelowItsLabel_isStillRead() {
        val receipt = ReceiptAnalyzer.analyze(totalOnNextLine)!!

        assertEquals(11.25, receipt.total, 0.001)
        assertTrue(receipt.totalWasLabelled)
        // The subtotal shares the value but must not become an item.
        assertEquals(2, receipt.items.size)
        assertEquals("Espresso", receipt.items[0].description)
        assertEquals(2.0, receipt.items[0].quantity!!, 0.001)
    }

    @Test
    fun analyze_serviceFee_countsAsAnItemSoTheItemsAddUp() {
        val restaurant = RecognizedDocument(
            line(0.05f, 0.20f to "Restaurante do Ze") +
                line(0.10f, 0.20f to "Data 02/04/2024") +
                line(0.20f, 0.05f to "Prato feito", 0.85f to "32,00") +
                line(0.25f, 0.05f to "Suco de laranja", 0.85f to "8,00") +
                line(0.32f, 0.05f to "Subtotal", 0.85f to "40,00") +
                line(0.37f, 0.05f to "Taxa de servico 10%", 0.85f to "4,00") +
                line(0.43f, 0.05f to "TOTAL A PAGAR", 0.85f to "44,00")
        )

        val receipt = ReceiptAnalyzer.analyze(restaurant)!!

        assertEquals(44.00, receipt.total, 0.001)
        assertEquals(3, receipt.items.size)
        assertTrue(receipt.itemsMatchTotal)
    }

    @Test
    fun analyze_photoWithoutAnyAmount_isNotAReceipt() {
        val poster = RecognizedDocument(line(0.05f, 0.20f to "Feira de domingo na praça"))

        assertNull(ReceiptAnalyzer.analyze(poster))
    }

    @Test
    fun analyze_emptyPage_isNotAReceipt() {
        assertNull(ReceiptAnalyzer.analyze(RecognizedDocument(emptyList())))
    }
}
