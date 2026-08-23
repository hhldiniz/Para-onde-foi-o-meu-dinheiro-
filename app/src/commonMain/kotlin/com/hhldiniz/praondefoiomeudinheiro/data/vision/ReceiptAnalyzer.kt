package com.hhldiniz.praondefoiomeudinheiro.data.vision

import com.hhldiniz.praondefoiomeudinheiro.data.local.TransactionValueParser
import com.hhldiniz.praondefoiomeudinheiro.domain.vision.DetectedReceipt
import com.hhldiniz.praondefoiomeudinheiro.domain.vision.ReceiptItem
import com.hhldiniz.praondefoiomeudinheiro.domain.vision.RecognizedDocument
import com.hhldiniz.praondefoiomeudinheiro.platform.currentTimeMillis
import com.hhldiniz.praondefoiomeudinheiro.util.localDateOf
import com.hhldiniz.praondefoiomeudinheiro.util.normalizeForMatching
import com.hhldiniz.praondefoiomeudinheiro.util.startOfDayMillis

/**
 * Reads a photographed receipt ("nota fiscal", "cupom fiscal", NFC-e) into a
 * single reviewable expense.
 *
 * This is the other half of the automatic importer. [TransactionFieldClassifier]
 * assumes the page *is* a table — many rows, each one a transaction, columns
 * that mean the same thing all the way down. A receipt is the opposite shape:
 * one purchase, printed as a header (who sold it, when), a ragged list of
 * items, and a total that is the only number that matters. Run through the
 * table classifier it yields nonsense — the item lines have no date column and
 * the amounts belong to a single purchase — so it gets its own reader.
 *
 * Everything here works off the lines [DocumentLayoutAnalyzer.lines] rebuilds
 * from the recognizer's words, and off the labels the receipt prints in
 * Portuguese/English/Spanish. Nothing is bank- or store-specific: the total is
 * whichever line carries the strongest "total" label, the items are the priced
 * lines above it, and the merchant is the first line of the header that is
 * neither an address nor a document number.
 */
object ReceiptAnalyzer {

    /** How far down the page the merchant name may still be found. */
    private const val MERCHANT_SEARCH_DEPTH = 8

    /** Longest merchant name kept; anything beyond is a paragraph, not a name. */
    private const val MERCHANT_MAX_LENGTH = 60

    /** Confidence multiplier when the total had to be guessed as the largest value. */
    private const val UNLABELLED_TOTAL_PENALTY = 0.75f

    /** Confidence multiplier when the receipt carried no readable date. */
    private const val MISSING_DATE_PENALTY = 0.85f

    /** Confidence multiplier when no merchant name could be told apart from the header. */
    private const val MISSING_MERCHANT_PENALTY = 0.9f

    /** Labels that name the amount actually due, strongest first. */
    private val STRONG_TOTAL_LABELS = listOf(
        "valor a pagar", "total a pagar", "valor total", "total geral", "total da compra",
        "total da nota", "total liquido", "importe total", "total amount", "amount due",
        "total to pay",
    )

    /** The bare word, which only wins when no stronger label is present. */
    private val WEAK_TOTAL_LABELS = listOf("total", "importe")

    /**
     * Lines that carry a number but never the purchase total: partial sums,
     * item counts, taxes and how the bill was settled. "Qtd. total de itens"
     * and "Subtotal" both contain "total", which is exactly why this list is
     * checked first.
     */
    private val NEVER_TOTAL = listOf(
        "subtotal", "sub total", "sub-total", "qtd", "quantidade", "itens", "items",
        "troco", "desconto", "descontos", "acrescimo", "tributo", "imposto", "impuesto",
        "aproximado", "lei 12.741", "cambio", "change",
        "forma de pagamento", "formas de pagamento", "dinheiro", "cartao", "credito",
        "debito", "pix", "vale", "valor pago", "valor recebido", "cash", "efectivo",
    )

    /** Header/footer boilerplate that is never a purchased item nor the merchant's name. */
    private val LINE_NOISE = listOf(
        "cnpj", "cpf", "c.n.p.j", "c.p.f", "insc", "inscricao", "ie:", "im:", "cep",
        "rua ", "av ", "av.", "avenida", "alameda", "travessa", "rodovia", "bairro",
        "telefone", "fone", "tel:", "www", "http", "@",
        "danfe", "documento auxiliar", "nota fiscal", "cupom fiscal", "nfc-e", "nfce",
        "nf-e", "sat", "serie", "protocolo", "autorizacao", "chave de acesso",
        "emissao", "operador", "caixa", "consumidor", "codigo", "descricao",
        "obrigado", "volte sempre", "vl unit", "vl total", "preco unit",
    )

    /** Units and separators that trail an item's description instead of describing it. */
    private val UNIT_TOKENS = setOf(
        "x", "un", "und", "unid", "uni", "kg", "g", "gr", "l", "lt", "ml", "pc", "pct",
        "cx", "dz", "m", "m2", "pç", "pc.", "ea",
    )

    /** Words that place a purchase in one of the app's default categories. */
    private val CATEGORY_KEYWORDS: List<Pair<String, List<String>>> = listOf(
        "Alimentacao" to listOf(
            "mercado", "supermercado", "mercearia", "atacad", "padaria", "panificadora",
            "acougue", "hortifruti", "restaurante", "lanchonete", "pizzaria", "churrascaria",
            "cafe", "sorveteria", "doceria", "emporio", "quitanda", "food", "burger",
            "arroz", "feijao", "leite", "pao", "refrigerante", "cerveja",
            "coffee", "bakery", "grocery", "market", "restaurant", "diner", "deli",
            "panaderia", "carniceria", "comida",
        ),
        "Transporte" to listOf(
            "posto", "combustivel", "gasolina", "etanol", "alcool", "diesel", "uber",
            "taxi", "estacionamento", "pedagio", "oficina", "auto center", "pneu",
            "passagem", "rodoviaria", "metro", "onibus",
            "fuel", "gas station", "parking", "gasolinera", "aparcamiento",
        ),
        "Saude" to listOf(
            "farmacia", "drogaria", "droga ", "clinica", "laboratorio", "hospital",
            "odonto", "dentista", "otica", "exame", "remedio", "generico",
            "pharmacy", "drugstore", "farmacia ", "clinic",
        ),
        "Educacao" to listOf(
            "livraria", "papelaria", "escola", "colegio", "faculdade", "universidade",
            "curso", "caderno", "apostila",
        ),
        "Lazer" to listOf(
            "cinema", "teatro", "show", "parque", "games", "streaming", "clube",
            "hotel", "pousada", "viagem", "brinquedo",
        ),
        "Moradia" to listOf(
            "aluguel", "condominio", "energia", "eletrica", "agua", "saneamento", "gas ",
            "internet", "construcao", "material de construcao", "moveis", "eletrodomestico",
        ),
    )

    /** One rebuilt line of the receipt with its number already pulled out. */
    private data class ReceiptLine(
        val text: String,
        val normalized: String,
        val tokens: List<String>,
        /** Index of the token holding the line's price, or -1 when it has none. */
        val amountIndex: Int,
        val amount: Double?,
        val rawAmount: String,
        val dateToken: String?,
        val dateMillis: Long?,
        val confidence: Float,
    ) {
        fun mentions(markers: List<String>) = markers.any { normalized.contains(it) }
    }

    /**
     * Reads [document] as a receipt, or returns null when it carries no amount
     * at all — a photo of something that is not a receipt, or one too blurred
     * for the recognizer to find a single price in.
     */
    fun analyze(document: RecognizedDocument): DetectedReceipt? {
        val lines = DocumentLayoutAnalyzer.lines(document).map(::toReceiptLine)
        if (lines.isEmpty()) return null

        val total = findTotal(lines) ?: return null
        val items = extractItems(lines, total)
        val merchant = findMerchant(lines)
        val date = findDate(lines)
        val category = guessCategory(merchant, items)

        var confidence = document.averageConfidence.takeIf { it > 0f } ?: 1f
        if (!total.labelled) confidence *= UNLABELLED_TOTAL_PENALTY
        if (date == null) confidence *= MISSING_DATE_PENALTY
        if (merchant.isBlank()) confidence *= MISSING_MERCHANT_PENALTY

        return DetectedReceipt(
            merchant = merchant,
            documentId = findDocumentId(lines),
            dateMillis = date?.millis ?: today(),
            total = total.value,
            items = items,
            category = category,
            confidence = confidence.coerceIn(0f, 1f),
            rawTotal = total.raw,
            rawDate = date?.raw.orEmpty(),
            totalWasLabelled = total.labelled,
        )
    }

    private fun toReceiptLine(cells: List<DocumentLayoutAnalyzer.Cell>): ReceiptLine {
        val tokens = cells.flatMap { cell -> cell.text.split(' ') }.filter { it.isNotBlank() }
        val amountIndex = tokens.indexOfLast { TransactionValueParser.looksLikeMoney(it) }
        val amount = tokens.getOrNull(amountIndex)?.let { TransactionValueParser.parseAmount(it) }
        val dateToken = tokens.firstOrNull { TransactionValueParser.looksLikeDate(it) }
        val text = tokens.joinToString(" ")
        return ReceiptLine(
            text = text,
            normalized = normalizeForMatching(text),
            tokens = tokens,
            amountIndex = amountIndex,
            // A receipt prints what was spent; the sign it may carry is noise.
            amount = amount?.let { kotlin.math.abs(it) },
            rawAmount = tokens.getOrNull(amountIndex).orEmpty(),
            dateToken = dateToken,
            dateMillis = dateToken?.let { TransactionValueParser.parseDate(it) },
            confidence = if (cells.isEmpty()) 1f else cells.map { it.confidence }.average().toFloat(),
        )
    }

    private data class Total(val value: Double, val raw: String, val lineIndex: Int, val labelled: Boolean)

    /**
     * The total is the line with the strongest "total" label and a number on
     * it (or on the line right below, for receipts that right-align the value
     * onto its own line). With no label anywhere, the largest amount that is
     * not a tax, a payment or a partial sum stands in — flagged, so the UI can
     * say the reading is a guess.
     */
    private fun findTotal(lines: List<ReceiptLine>): Total? {
        var best: Total? = null
        var bestRank = 0

        lines.forEachIndexed { index, line ->
            if (line.mentions(NEVER_TOTAL)) return@forEachIndexed
            val rank = when {
                line.mentions(STRONG_TOTAL_LABELS) -> 2
                line.mentions(WEAK_TOTAL_LABELS) -> 1
                else -> 0
            }
            if (rank == 0) return@forEachIndexed

            val (value, raw) = line.amount?.let { it to line.rawAmount }
                ?: lines.getOrNull(index + 1)
                    ?.takeIf { next -> next.amount != null && !next.mentions(NEVER_TOTAL) }
                    ?.let { next -> next.amount!! to next.rawAmount }
                ?: return@forEachIndexed

            // Later lines win ties: the grand total is printed below any
            // intermediate one that shares its label.
            if (rank >= bestRank) {
                bestRank = rank
                best = Total(value, raw, index, labelled = true)
            }
        }

        best?.let { return it }

        return lines.withIndex()
            .filter { (_, line) -> !line.mentions(NEVER_TOTAL) && line.amount != null }
            .maxByOrNull { (_, line) -> line.amount!! }
            ?.let { (index, line) -> Total(line.amount!!, line.rawAmount, index, labelled = false) }
    }

    /**
     * The priced lines above the total, with their codes, quantities and unit
     * prices stripped back down to what was bought.
     */
    private fun extractItems(lines: List<ReceiptLine>, total: Total): List<ReceiptItem> {
        val limit = if (total.labelled) total.lineIndex else lines.size
        return lines.take(limit).mapIndexedNotNull { index, line ->
            val amount = line.amount ?: return@mapIndexedNotNull null
            if (amount <= 0.0) return@mapIndexedNotNull null
            if (line.mentions(NEVER_TOTAL) || line.mentions(WEAK_TOTAL_LABELS)) return@mapIndexedNotNull null
            if (line.mentions(LINE_NOISE) || line.dateToken != null) return@mapIndexedNotNull null
            // Only the total may equal the total; a line matching it above is
            // a partial sum the labels did not catch.
            if (!total.labelled && index != total.lineIndex && amount == total.value) {
                return@mapIndexedNotNull null
            }
            if (amount > total.value) return@mapIndexedNotNull null

            val (description, quantity) = describeItem(line)
            if (letterCount(description) < 3) return@mapIndexedNotNull null

            ReceiptItem(
                description = description,
                amount = amount,
                quantity = quantity,
                confidence = line.confidence,
            )
        }
    }

    /**
     * Splits an item line into what was bought and how many of it, dropping
     * the price itself, the `2 UN x 12,90` tail some receipts print after the
     * description, and the item code or barcode in front of it.
     *
     * That tail is matched as a shape — a unit price, a multiplier, a unit and
     * a quantity, in that order from the right, each of them optional — rather
     * than by eating every trailing number: "ARROZ TIPO 1" ends in a number
     * that is part of the product's name.
     */
    private fun describeItem(line: ReceiptLine): Pair<String, Double?> {
        val kept = line.tokens.toMutableList()
        if (line.amountIndex in kept.indices) kept.removeAt(line.amountIndex)

        fun popIf(predicate: (String) -> Boolean): String? =
            kept.lastOrNull()?.takeIf(predicate)?.also { kept.removeAt(kept.size - 1) }

        val unitPrice = popIf { TransactionValueParser.looksLikeMoney(it) }
        val multiplier = popIf { normalizeForMatching(it) == "x" }
        val unit = popIf { normalizeForMatching(it) in UNIT_TOKENS }
        var quantity = if (unitPrice != null || multiplier != null || unit != null) {
            popIf {
                TransactionValueParser.looksLikeNumber(it) && !TransactionValueParser.looksLikeMoney(it)
            }?.let { TransactionValueParser.parseAmount(it) }
        } else {
            null
        }

        // The item number or barcode printed in front of the description, and
        // the quantity some layouts put there instead ("2 x PÃO 5,00"). A long
        // or zero-padded run of digits is a code, never a count.
        while (kept.size > 1) {
            val first = kept.first()
            val isCode = first.all { it.isDigit() }
            if (!isCode && normalizeForMatching(first) !in UNIT_TOKENS) break
            kept.removeAt(0)
            if (quantity == null && isCode && first.length <= 2 && !first.startsWith("0")) {
                quantity = first.toDoubleOrNull()
            }
        }

        return kept.joinToString(" ").trim() to quantity
    }

    /**
     * The first header line that reads like a name: no price, no date, and
     * none of the address/document boilerplate printed around it.
     */
    private fun findMerchant(lines: List<ReceiptLine>): String {
        val candidate = lines.take(MERCHANT_SEARCH_DEPTH).firstOrNull { line ->
            line.amount == null &&
                line.dateToken == null &&
                !line.mentions(LINE_NOISE) &&
                letterCount(line.text) >= 3 &&
                line.tokens.any { letterCount(it) >= 3 }
        } ?: return ""
        return candidate.text.take(MERCHANT_MAX_LENGTH).trim()
    }

    /** The date next to an "emissão"/"data" label, or the first one printed. */
    private fun findDate(lines: List<ReceiptLine>): DateReading? {
        val labelled = lines.firstOrNull { line ->
            line.dateMillis != null &&
                (line.normalized.contains("emissao") || line.normalized.contains("data"))
        }
        val line = labelled ?: lines.firstOrNull { it.dateMillis != null } ?: return null
        return DateReading(line.dateMillis!!, line.dateToken.orEmpty())
    }

    private data class DateReading(val millis: Long, val raw: String)

    /** The issuer's CNPJ/CPF as printed, empty when the receipt carried none. */
    private fun findDocumentId(lines: List<ReceiptLine>): String {
        val line = lines.firstOrNull { it.normalized.contains("cnpj") || it.normalized.contains("c.n.p.j") }
            ?: return ""
        return line.tokens.firstOrNull { token ->
            token.count { it.isDigit() } >= 11 && token.none { it.isLetter() }
        }.orEmpty()
    }

    /**
     * Matches the merchant and the item descriptions against the keyword sets
     * of the app's default categories, and returns the one hit most often
     * (blank when nothing matches — the user picks in that case).
     */
    fun guessCategory(merchant: String, items: List<ReceiptItem>): String {
        val merchantText = normalizeForMatching(merchant)
        val itemsText = normalizeForMatching(items.joinToString(" ") { it.description })

        var best = ""
        var bestScore = 0
        for ((category, keywords) in CATEGORY_KEYWORDS) {
            // The merchant names the purchase far better than any single item
            // does, so a hit there outweighs the item lines.
            val score = keywords.sumOf { keyword ->
                (if (merchantText.contains(keyword)) 3 else 0) +
                    (if (itemsText.contains(keyword)) 1 else 0)
            }
            if (score > bestScore) {
                bestScore = score
                best = category
            }
        }
        return best
    }

    private fun letterCount(text: String) = text.count { it.isLetter() }

    private fun today(): Long {
        val date = localDateOf(currentTimeMillis())
        return startOfDayMillis(date.year, date.month, date.day)
    }
}
