package com.hhldiniz.praondefoiomeudinheiro.data.vision

import com.hhldiniz.praondefoiomeudinheiro.domain.vision.TransactionField
import com.hhldiniz.praondefoiomeudinheiro.util.localDateOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The heart of the automatic import: given a table whose layout nobody
 * described in advance, does the classifier work out what each column means?
 *
 * Every case here is a layout the app has to survive — its own export, a
 * headerless receipt, statements in three languages, a debit/credit pair, a
 * direction column, signed amounts.
 */
class TransactionFieldClassifierTest {

    private fun classify(rows: List<List<String>>) = TransactionFieldClassifier.classify(rows)

    @Test
    fun classify_headerlessTable_findsEveryFieldFromContentAlone() {
        val interpretation = classify(
            listOf(
                listOf("15/03/2024", "Mercado Bom Preço", "Alimentacao", "R$ 120,50"),
                listOf("16/03/2024", "Uber para o trabalho", "Transporte", "R$ 32,00"),
                listOf("17/03/2024", "Cinema com amigos", "Lazer", "R$ 45,00"),
                listOf("18/03/2024", "Padaria da Esquina", "Alimentacao", "R$ 18,90"),
            )
        )

        assertTrue(interpretation.isUsable)
        assertEquals(-1, interpretation.headerRowIndex)
        assertEquals(0, interpretation.columnOf(TransactionField.DATE))
        assertEquals(3, interpretation.columnOf(TransactionField.AMOUNT))
        assertEquals(1, interpretation.columnOf(TransactionField.DESCRIPTION))
        assertEquals(2, interpretation.columnOf(TransactionField.CATEGORY))
    }

    @Test
    fun classify_unusualColumnOrder_isNotAProblem() {
        // Amount first, date last: nothing here is position-based.
        val interpretation = classify(
            listOf(
                listOf("120,50", "Mercado Bom Preço", "15/03/2024"),
                listOf("32,00", "Uber para o trabalho", "16/03/2024"),
                listOf("45,00", "Cinema com amigos", "17/03/2024"),
            )
        )

        assertEquals(2, interpretation.columnOf(TransactionField.DATE))
        assertEquals(0, interpretation.columnOf(TransactionField.AMOUNT))
        assertEquals(1, interpretation.columnOf(TransactionField.DESCRIPTION))
    }

    @Test
    fun classify_headerRow_isFoundAndExcludedFromTheData() {
        val interpretation = classify(
            listOf(
                listOf("Extrato de março"),
                listOf("Data", "Descrição", "Categoria", "Valor"),
                listOf("15/03/2024", "Mercado Bom Preço", "Alimentacao", "R$ 120,50"),
                listOf("16/03/2024", "Uber para o trabalho", "Transporte", "R$ 32,00"),
            )
        )

        assertEquals(1, interpretation.headerRowIndex)
        assertEquals(2, interpretation.dataRows.size)
        assertEquals(0, interpretation.columnOf(TransactionField.DATE))
        assertEquals(3, interpretation.columnOf(TransactionField.AMOUNT))
        assertEquals("Valor", interpretation.mappings.first { it.field == TransactionField.AMOUNT }.header)
    }

    @Test
    fun classify_englishAndSpanishHeaders_workTheSameWay() {
        val english = classify(
            listOf(
                listOf("Date", "Description", "Amount"),
                listOf("2024-03-15", "Grocery store", "120.50"),
                listOf("2024-03-16", "Ride to work", "32.00"),
            )
        )
        assertEquals(0, english.columnOf(TransactionField.DATE))
        assertEquals(2, english.columnOf(TransactionField.AMOUNT))

        val spanish = classify(
            listOf(
                listOf("Fecha", "Concepto", "Importe"),
                listOf("15/03/2024", "Supermercado del barrio", "120,50"),
                listOf("16/03/2024", "Viaje al trabajo", "32,00"),
            )
        )
        assertEquals(0, spanish.columnOf(TransactionField.DATE))
        assertEquals(2, spanish.columnOf(TransactionField.AMOUNT))
    }

    @Test
    fun extract_producesTransactionsWithParsedValues() {
        val interpretation = classify(
            listOf(
                listOf("Data", "Descrição", "Categoria", "Valor"),
                listOf("15/03/2024", "Mercado Bom Preço", "Alimentacao", "R$ 120,50"),
                listOf("16/03/2024", "Uber para o trabalho", "Transporte", "R$ 32,00"),
            )
        )

        val transactions = TransactionFieldClassifier.extract(interpretation)

        assertEquals(2, transactions.size)
        val first = transactions.first()
        assertEquals(120.50, first.amount, 0.001)
        assertEquals("Mercado Bom Preço", first.description)
        assertEquals("Alimentacao", first.category)
        assertTrue(first.isExpense)
        val date = localDateOf(first.dateMillis)
        assertEquals(2024, date.year)
        assertEquals(3, date.month)
        assertEquals(15, date.day)
    }

    @Test
    fun extract_debitCreditColumns_splitExpensesFromIncome() {
        val interpretation = classify(
            listOf(
                listOf("Data", "Histórico", "Débito", "Crédito"),
                listOf("01/04/2024", "Compra supermercado", "150,00", ""),
                listOf("02/04/2024", "Pagamento de salario", "", "3.000,00"),
                listOf("03/04/2024", "Conta de luz", "220,45", ""),
                listOf("04/04/2024", "Reembolso viagem", "", "80,00"),
            )
        )

        assertEquals(2, interpretation.columnOf(TransactionField.AMOUNT))
        assertEquals(3, interpretation.columnOf(TransactionField.CREDIT_AMOUNT))

        val transactions = TransactionFieldClassifier.extract(interpretation)
        assertEquals(4, transactions.size)
        assertTrue(transactions[0].isExpense)
        assertFalse(transactions[1].isExpense)
        assertEquals(3000.0, transactions[1].amount, 0.001)
        assertTrue(transactions[2].isExpense)
        assertFalse(transactions[3].isExpense)
    }

    @Test
    fun extract_directionColumn_beatsEveryOtherSignal() {
        val interpretation = classify(
            listOf(
                listOf("Data", "Histórico", "Valor", "Tipo"),
                listOf("01/04/2024", "Compra supermercado", "150,00", "D"),
                listOf("02/04/2024", "Deposito recebido", "3.000,00", "C"),
                listOf("03/04/2024", "Conta de luz", "220,45", "D"),
                listOf("04/04/2024", "Transferencia recebida", "80,00", "C"),
            )
        )

        assertEquals(3, interpretation.columnOf(TransactionField.TYPE))
        // The direction column must not be mistaken for the category column.
        assertNull(interpretation.columnOf(TransactionField.CATEGORY))

        val transactions = TransactionFieldClassifier.extract(interpretation)
        assertTrue(transactions[0].isExpense)
        assertFalse(transactions[1].isExpense)
        assertTrue(transactions[2].isExpense)
        assertFalse(transactions[3].isExpense)
    }

    @Test
    fun extract_mixedSigns_meanNegativeIsAnExpense() {
        val interpretation = classify(
            listOf(
                listOf("Date", "Description", "Amount"),
                listOf("01/04/2024", "Grocery store", "-150,00"),
                listOf("02/04/2024", "Monthly salary", "3.000,00"),
                listOf("03/04/2024", "Electricity bill", "-220,45"),
            )
        )

        val transactions = TransactionFieldClassifier.extract(interpretation)

        assertTrue(transactions[0].isExpense)
        assertFalse(transactions[1].isExpense)
        assertTrue(transactions[2].isExpense)
        // Magnitudes are stored positive; direction lives in the flag.
        assertEquals(150.0, transactions[0].amount, 0.001)
    }

    @Test
    fun extract_allPositiveAmounts_areAllExpenses() {
        val interpretation = classify(
            listOf(
                listOf("15/03/2024", "Mercado Bom Preço", "R$ 120,50"),
                listOf("16/03/2024", "Uber para o trabalho", "R$ 32,00"),
            )
        )

        val transactions = TransactionFieldClassifier.extract(interpretation)

        assertTrue(transactions.all { it.isExpense })
    }

    @Test
    fun extract_skipsRowsWhoseDateOrAmountDoesNotParse() {
        val interpretation = classify(
            listOf(
                listOf("Data", "Descrição", "Valor"),
                listOf("15/03/2024", "Mercado Bom Preço", "R$ 120,50"),
                listOf("16/03/2024", "Uber para o trabalho", "R$ 32,00"),
                listOf("TOTAL", "", "R$ 152,50"),
            )
        )

        val transactions = TransactionFieldClassifier.extract(interpretation)

        assertEquals(2, transactions.size)
    }

    @Test
    fun classify_tableWithoutTransactions_isNotUsable() {
        val interpretation = classify(
            listOf(
                listOf("Produto", "Quantidade"),
                listOf("Arroz", "2"),
                listOf("Feijão", "1"),
            )
        )

        assertFalse(interpretation.isUsable)
        assertTrue(TransactionFieldClassifier.extract(interpretation).isEmpty())
    }

    @Test
    fun classify_confidenceIsHigherWithAHeaderThanWithout() {
        val rows = listOf(
            listOf("15/03/2024", "Mercado Bom Preço", "R$ 120,50"),
            listOf("16/03/2024", "Uber para o trabalho", "R$ 32,00"),
        )
        val withoutHeader = classify(rows)
        val withHeader = classify(listOf(listOf("Data", "Descrição", "Valor")) + rows)

        assertTrue(withHeader.confidence > withoutHeader.confidence)
        assertTrue(withoutHeader.confidence > 0.5f)
    }

    @Test
    fun classify_ocrNoise_doesNotDerailTheMapping() {
        // Extra empty cells and a stray column are what a photographed page
        // regularly produces; the required fields must still be found.
        val interpretation = classify(
            listOf(
                listOf("", "Data", "Descrição", "", "Valor"),
                listOf("", "15/03/2024", "Mercado Bom Preço", "", "R$ 120,50"),
                listOf("*", "16/03/2024", "Uber para o trabalho", "", "R$ 32,00"),
            )
        )

        assertEquals(1, interpretation.columnOf(TransactionField.DATE))
        assertEquals(4, interpretation.columnOf(TransactionField.AMOUNT))
        assertEquals(2, TransactionFieldClassifier.extract(interpretation).size)
    }
}
