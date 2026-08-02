package com.hhldiniz.praondefoiomeudinheiro.data.vision

import com.hhldiniz.praondefoiomeudinheiro.data.local.TransactionValueParser
import com.hhldiniz.praondefoiomeudinheiro.domain.vision.DetectedTransaction
import com.hhldiniz.praondefoiomeudinheiro.domain.vision.FieldMapping
import com.hhldiniz.praondefoiomeudinheiro.domain.vision.TransactionField
import com.hhldiniz.praondefoiomeudinheiro.util.normalizeForMatching
import kotlin.math.exp
import kotlin.math.min

/**
 * Decides what each column of an unknown table holds — date, amount,
 * description, category — from the *content* of its cells, so an import no
 * longer depends on the file following the app's own layout.
 *
 * ### The model
 *
 * Every column is reduced to a fixed vector of features (share of cells that
 * parse as a date, as money, as free text; how repetitive and how wordy the
 * column is; whether a header cell names a known field — see [ColumnFeatures]).
 * Each candidate role scores that vector through a linear function whose
 * weights encode what distinguishes the roles from one another: dates are the
 * only column that parses as dates, amounts are numeric and signed, a
 * description is wordy and almost never repeats, a category is short text that
 * repeats a lot. The weights below were calibrated by hand against the layouts
 * this app has to cope with (its own export, Brazilian/US/Spanish bank
 * statements, photographed receipts) and are the one place to tune when a new
 * layout confuses the importer — no per-bank special cases elsewhere.
 *
 * Roles are then assigned jointly rather than greedily: the combination of
 * columns with the highest total score wins, so one strong column cannot claim
 * a role that another column explains better overall. A date and an amount
 * column are required; description and category stay unassigned when nothing
 * scores well enough for them, and their cells simply come out blank.
 *
 * [TransactionColumnMapper][com.hhldiniz.praondefoiomeudinheiro.data.local.TransactionColumnMapper]
 * stays in charge of the *direct* import path, where header names are known to
 * follow the expected format; this classifier is what the automatic path uses
 * when they are not.
 */
object TransactionFieldClassifier {

    // -- Header vocabulary ---------------------------------------------------
    // Matched token-by-token (not whole-cell) so compound headers such as
    // "Data do lançamento" or "Valor (R$)" are still recognized.

    private val DATE_TOKENS = setOf("data", "date", "dia", "fecha", "day", "dt", "datas")
    private val AMOUNT_TOKENS = setOf(
        "valor", "value", "amount", "montante", "importe", "preco", "price", "quantia",
        "debito", "debit", "saida", "despesa", "despesas", "gasto", "gastos", "cargo", "egreso",
    )
    private val CREDIT_TOKENS = setOf(
        "credito", "credit", "receita", "receitas", "renda", "rendas", "entrada", "entradas",
        "income", "deposito", "deposit", "ingreso", "haber",
    )
    private val DESCRIPTION_TOKENS = setOf(
        "descricao", "description", "descripcion", "historico", "detalhe", "detalhes",
        "observacao", "observacoes", "obs", "memo", "note", "notes", "lancamento", "lancamentos",
        "movimento", "movimentacao", "estabelecimento", "titulo", "concepto", "referencia",
        "transacao", "transaction", "merchant",
    )
    private val CATEGORY_TOKENS = setOf(
        "categoria", "category", "tipo", "classe", "class", "grupo", "rubrica", "clasificacion",
    )

    /** Words a dedicated direction column uses for money going out… */
    private val EXPENSE_WORDS = setOf(
        "d", "debito", "debit", "despesa", "gasto", "saida", "pagamento", "compra",
        "expense", "withdrawal", "egreso", "cargo", "retirada",
    )

    /** …and for money coming in. */
    private val INCOME_WORDS = setOf(
        "c", "credito", "credit", "receita", "renda", "entrada", "deposito", "salario",
        "income", "deposit", "ingreso", "abono",
    )

    private val TOKEN_SPLIT = Regex("""[^0-9A-Za-zÀ-ÖØ-öø-ÿ]+""")

    // -- Thresholds ----------------------------------------------------------

    /** How many rows from the top are considered when looking for a header. */
    private const val HEADER_SEARCH_DEPTH = 20

    /** Minimum score for the two required roles; below it the table is not a transaction table. */
    private const val MIN_REQUIRED_SCORE = 0.8f

    /** Minimum score for description/category, which are better left blank than guessed. */
    private const val MIN_OPTIONAL_SCORE = 0.9f

    /** Columns considered per role during the joint assignment search. */
    private const val CANDIDATES_PER_FIELD = 6

    /** Score mapping to a `0f..1f` confidence; a score of this much reads as "even odds". */
    private const val CONFIDENCE_MIDPOINT = 1.2f

    /** Share of rows that must fill exactly one of two amount columns to read them as debit/credit. */
    private const val MIN_EXCLUSIVITY = 0.7f

    /** Share of a column's cells that must be direction words for it to be the [TransactionField.TYPE] column. */
    private const val MIN_TYPE_WORD_RATIO = 0.6f

    /** The measurements a single column is judged on. All ratios are `0f..1f`. */
    data class ColumnFeatures(
        val index: Int,
        val header: String,
        val headerField: TransactionField?,
        val filledRatio: Float,
        val dateRatio: Float,
        val moneyRatio: Float,
        val numberRatio: Float,
        val textRatio: Float,
        val signedRatio: Float,
        val typeWordRatio: Float,
        /** Mean words per cell, clamped into `0f..1f` at four words ("wordiness"). */
        val wordiness: Float,
        /** Share of *distinct* values; low means the column repeats itself, like a category. */
        val uniqueRatio: Float,
    )

    /** What the classifier made of a table. */
    data class Interpretation(
        /** Index into the original rows, or -1 when the table has no header row. */
        val headerRowIndex: Int,
        val dataRows: List<List<String>>,
        val mappings: List<FieldMapping>,
        val features: List<ColumnFeatures>,
        val confidence: Float,
    ) {
        val isUsable: Boolean
            get() = mappings.any { it.field == TransactionField.DATE } &&
                mappings.any { it.field == TransactionField.AMOUNT }

        fun columnOf(field: TransactionField): Int? =
            mappings.firstOrNull { it.field == field }?.columnIndex
    }

    /** Runs header detection, feature extraction and role assignment over [rows]. */
    fun classify(rows: List<List<String>>): Interpretation {
        val headerRowIndex = findHeaderRowIndex(rows)
        val candidateRows = if (headerRowIndex >= 0) rows.drop(headerRowIndex + 1) else rows
        val dataRows = candidateRows.filter { isDataRow(it) }
        if (dataRows.isEmpty()) {
            return Interpretation(headerRowIndex, emptyList(), emptyList(), emptyList(), 0f)
        }

        val header = rows.getOrNull(headerRowIndex).orEmpty()
        val columnCount = maxOf(dataRows.maxOf { it.size }, header.size)
        val features = (0 until columnCount).map { extractFeatures(it, header, dataRows) }

        val scores = TransactionField.entries.associateWith { field ->
            FloatArray(columnCount) { column -> score(field, features[column]) }
        }

        // A column made of direction words ("D"/"C", "entrada"/"saída") is
        // taken out before the core search: it is short repeated text, which
        // otherwise makes a convincing category column and would swallow the
        // very signal that tells expenses from income apart.
        val typeColumn = features
            .filter { it.typeWordRatio >= MIN_TYPE_WORD_RATIO }
            .maxByOrNull { it.typeWordRatio }
            ?.index

        val assignment = bestAssignment(scores, columnCount, setOfNotNull(typeColumn)).toMutableMap()
        if (assignment.isNotEmpty() && typeColumn != null) {
            assignment[TransactionField.TYPE] = typeColumn
        }
        assignCreditColumn(assignment, features, dataRows, scores)

        val mappings = assignment.map { (field, column) ->
            FieldMapping(
                field = field,
                columnIndex = column,
                header = header.getOrElse(column) { "" }.trim(),
                confidence = confidenceOf(scores.getValue(field)[column]),
            )
        }.sortedBy { it.columnIndex }

        val required = mappings.filter { it.field == TransactionField.DATE || it.field == TransactionField.AMOUNT }
        val confidence = if (required.size < 2) 0f else required.map { it.confidence }.average().toFloat()

        return Interpretation(headerRowIndex, dataRows, mappings, features, confidence)
    }

    /**
     * Turns the classified rows into transaction proposals, dropping rows
     * whose date or amount does not actually parse.
     *
     * [baseConfidence] is the recognizer's own confidence in the text (1f for
     * files, which are read exactly); it caps every row's confidence, since no
     * amount of clean structure rescues a misread digit.
     */
    fun extract(
        interpretation: Interpretation,
        baseConfidence: Float = 1f,
        fallbackYear: Int? = null,
    ): List<DetectedTransaction> {
        val dateColumn = interpretation.columnOf(TransactionField.DATE) ?: return emptyList()
        val amountColumn = interpretation.columnOf(TransactionField.AMOUNT) ?: return emptyList()
        val creditColumn = interpretation.columnOf(TransactionField.CREDIT_AMOUNT)
        val typeColumn = interpretation.columnOf(TransactionField.TYPE)
        val descriptionColumn = interpretation.columnOf(TransactionField.DESCRIPTION)
        val categoryColumn = interpretation.columnOf(TransactionField.CATEGORY)

        val mappingConfidence = interpretation.mappings
            .filter { it.field == TransactionField.DATE || it.field == TransactionField.AMOUNT }
            .map { it.confidence }
            .ifEmpty { listOf(0f) }
            .average()
            .toFloat()

        // A column where every value is positive says nothing about direction,
        // so signs are only trusted when the column actually carries both.
        val signedRatio = interpretation.features.getOrNull(amountColumn)?.signedRatio ?: 0f
        val signsAreMeaningful = signedRatio > 0.05f && signedRatio < 0.95f

        return interpretation.dataRows.mapNotNull { row ->
            val rawDate = row.getOrElse(dateColumn) { "" }.trim()
            val dateMillis = TransactionValueParser.parseDate(rawDate, fallbackYear) ?: return@mapNotNull null

            val rawDebit = row.getOrElse(amountColumn) { "" }.trim()
            val rawCredit = creditColumn?.let { row.getOrElse(it) { "" }.trim() }.orEmpty()
            val usesCredit = rawDebit.isBlank() && rawCredit.isNotBlank()
            val rawAmount = if (usesCredit) rawCredit else rawDebit
            val amount = TransactionValueParser.parseAmount(rawAmount) ?: return@mapNotNull null

            val description = descriptionColumn?.let { row.getOrElse(it) { "" }.trim() }.orEmpty()
            val category = categoryColumn?.let { row.getOrElse(it) { "" }.trim() }.orEmpty()
            val typeWord = typeColumn?.let { row.getOrElse(it) { "" } }.orEmpty()

            val isExpense = resolveDirection(
                typeWord = typeWord,
                usesCreditColumn = usesCredit,
                hasCreditColumn = creditColumn != null,
                rawAmount = rawAmount,
                amount = amount,
                signsAreMeaningful = signsAreMeaningful,
            )

            var confidence = min(baseConfidence, mappingConfidence)
            if (description.isBlank()) confidence *= 0.9f
            if (category.isBlank()) confidence *= 0.95f

            DetectedTransaction(
                dateMillis = dateMillis,
                // The app stores magnitudes and keeps direction in `isExpense`.
                amount = if (amount < 0) -amount else amount,
                description = description,
                category = category,
                isExpense = isExpense,
                confidence = confidence,
                rawDate = rawDate,
                rawAmount = rawAmount,
            )
        }
    }

    /**
     * Direction is read from the strongest available signal: an explicit
     * direction column, then a debit/credit column pair, then a `D`/`C` marker
     * glued to the amount, then the sign — and, failing all of those, a
     * spending app's safest default: an expense.
     */
    private fun resolveDirection(
        typeWord: String,
        usesCreditColumn: Boolean,
        hasCreditColumn: Boolean,
        rawAmount: String,
        amount: Double,
        signsAreMeaningful: Boolean,
    ): Boolean {
        directionFromWord(typeWord)?.let { return it }
        if (hasCreditColumn) return !usesCreditColumn
        TransactionValueParser.debitCreditMarker(rawAmount)?.let { return it }
        if (signsAreMeaningful) return amount < 0
        return true
    }

    private fun directionFromWord(text: String): Boolean? {
        val normalized = normalizeForMatching(text)
        if (normalized.isEmpty()) return null
        if (normalized in EXPENSE_WORDS) return true
        if (normalized in INCOME_WORDS) return false
        val tokens = normalized.split(TOKEN_SPLIT).filter { it.isNotEmpty() }
        if (tokens.any { it in EXPENSE_WORDS }) return true
        if (tokens.any { it in INCOME_WORDS }) return false
        return null
    }

    // -- Header ---------------------------------------------------------------

    /**
     * The first row (within [HEADER_SEARCH_DEPTH]) that names at least two
     * different fields and is not itself a data row, or -1. Tables without a
     * header are perfectly importable — content features alone carry the
     * classification — the header just makes it more certain.
     */
    fun findHeaderRowIndex(rows: List<List<String>>): Int {
        val depth = min(rows.size, HEADER_SEARCH_DEPTH)
        for (index in 0 until depth) {
            val row = rows[index]
            if (isDataRow(row)) continue
            val named = row.mapNotNull { headerFieldFor(it) }.distinct()
            if (named.size >= 2) return index
        }
        return -1
    }

    /** The field a header cell names, or null when it names nothing recognizable. */
    fun headerFieldFor(cell: String): TransactionField? {
        val tokens = normalizeForMatching(cell).split(TOKEN_SPLIT).filter { it.isNotEmpty() }
        if (tokens.isEmpty()) return null
        // Credit is tested before amount because "crédito" is an amount word
        // too, and the pair only makes sense when the two are told apart.
        return when {
            tokens.any { it in DATE_TOKENS } -> TransactionField.DATE
            tokens.any { it in CREDIT_TOKENS } -> TransactionField.CREDIT_AMOUNT
            tokens.any { it in AMOUNT_TOKENS } -> TransactionField.AMOUNT
            tokens.any { it in CATEGORY_TOKENS } -> TransactionField.CATEGORY
            tokens.any { it in DESCRIPTION_TOKENS } -> TransactionField.DESCRIPTION
            else -> null
        }
    }

    /** A row is data when it carries both a date and a number somewhere. */
    fun isDataRow(row: List<String>): Boolean {
        val cells = row.map { it.trim() }.filter { it.isNotEmpty() }
        if (cells.size < 2) return false
        return cells.any { TransactionValueParser.looksLikeDate(it) } &&
            cells.any { TransactionValueParser.looksLikeNumber(it) }
    }

    // -- Features -------------------------------------------------------------

    private fun extractFeatures(
        index: Int,
        header: List<String>,
        dataRows: List<List<String>>,
    ): ColumnFeatures {
        val cells = dataRows.map { it.getOrElse(index) { "" }.trim() }
        val filled = cells.filter { it.isNotEmpty() }
        val headerCell = header.getOrElse(index) { "" }.trim()
        if (filled.isEmpty()) {
            return ColumnFeatures(
                index = index,
                header = headerCell,
                headerField = headerFieldFor(headerCell),
                filledRatio = 0f, dateRatio = 0f, moneyRatio = 0f, numberRatio = 0f,
                textRatio = 0f, signedRatio = 0f, typeWordRatio = 0f, wordiness = 0f,
                uniqueRatio = 0f,
            )
        }

        val count = filled.size.toFloat()
        val dates = filled.count { TransactionValueParser.looksLikeDate(it) }
        val money = filled.count { TransactionValueParser.looksLikeMoney(it) }
        val numbers = filled.count { TransactionValueParser.looksLikeNumber(it) }
        val text = filled.count { cell ->
            cell.count { it.isLetter() } >= 2 &&
                !TransactionValueParser.looksLikeDate(cell) &&
                !TransactionValueParser.looksLikeNumber(cell)
        }
        val signed = filled.count { cell ->
            cell.startsWith("-") || cell.endsWith("-") || (cell.startsWith("(") && cell.endsWith(")"))
        }
        val typeWords = filled.count { directionFromWord(it) != null }
        val words = filled.sumOf { cell -> cell.split(TOKEN_SPLIT).count { it.isNotEmpty() } }
        val distinct = filled.map { normalizeForMatching(it) }.distinct().size

        return ColumnFeatures(
            index = index,
            header = headerCell,
            headerField = headerFieldFor(headerCell),
            filledRatio = filled.size / cells.size.toFloat(),
            dateRatio = dates / count,
            moneyRatio = money / count,
            numberRatio = numbers / count,
            textRatio = text / count,
            signedRatio = signed / count,
            typeWordRatio = typeWords / count,
            wordiness = min(1f, (words / count) / 4f),
            uniqueRatio = distinct / count,
        )
    }

    // -- Scoring --------------------------------------------------------------

    /**
     * The linear model. Positive weights are evidence *for* a role, negative
     * ones are evidence against it; the negatives matter as much as the
     * positives, since what separates an amount from a date is largely that it
     * is *not* the other one.
     */
    fun score(field: TransactionField, features: ColumnFeatures): Float = with(features) {
        val headerBonus = if (headerField == field) 1.6f else 0f
        // A header naming a *different* field is strong evidence against this one.
        val headerPenalty = if (headerField != null && headerField != field) 0.8f else 0f
        val repetition = if (uniqueRatio > 0f) 1f - uniqueRatio else 0f

        val raw = when (field) {
            TransactionField.DATE ->
                3.2f * dateRatio + headerBonus - 1.2f * textRatio - 0.8f * moneyRatio

            TransactionField.AMOUNT, TransactionField.CREDIT_AMOUNT ->
                2.6f * moneyRatio + 1.1f * numberRatio + headerBonus +
                    0.4f * signedRatio - 2.2f * dateRatio - 1.4f * textRatio

            TransactionField.DESCRIPTION ->
                2.4f * textRatio + 1.2f * uniqueRatio + 0.9f * wordiness + headerBonus -
                    2.0f * dateRatio - 1.8f * moneyRatio - 1.0f * typeWordRatio

            TransactionField.CATEGORY ->
                1.8f * textRatio + 1.8f * repetition + headerBonus - 1.2f * wordiness -
                    1.6f * dateRatio - 1.8f * moneyRatio - 1.0f * typeWordRatio

            TransactionField.TYPE ->
                3.0f * typeWordRatio - 1.5f * dateRatio - 1.5f * moneyRatio
        }
        // A mostly empty column is a weak candidate for anything.
        (raw - headerPenalty) * (0.4f + 0.6f * filledRatio)
    }

    /** Squashes a score into `0f..1f`, with [CONFIDENCE_MIDPOINT] mapping to 0.5. */
    fun confidenceOf(score: Float): Float = 1f / (1f + exp(-(score - CONFIDENCE_MIDPOINT)))

    // -- Assignment -----------------------------------------------------------

    private val CORE_FIELDS = listOf(
        TransactionField.DATE,
        TransactionField.AMOUNT,
        TransactionField.DESCRIPTION,
        TransactionField.CATEGORY,
    )

    /**
     * Picks the combination of columns maximizing the total score, instead of
     * letting each role grab its own favourite: in a table where one column
     * scores highest for both description and category, the joint search gives
     * it to whichever role loses less by taking its runner-up.
     *
     * Only the [CANDIDATES_PER_FIELD] best columns per role are considered, so
     * the search stays bounded no matter how many columns a noisy scan
     * produces.
     */
    private fun bestAssignment(
        scores: Map<TransactionField, FloatArray>,
        columnCount: Int,
        excluded: Set<Int>,
    ): Map<TransactionField, Int> {
        val candidates = CORE_FIELDS.associateWith { field ->
            val minimum = if (field.isRequired()) MIN_REQUIRED_SCORE else MIN_OPTIONAL_SCORE
            (0 until columnCount)
                .filter { it !in excluded && scores.getValue(field)[it] >= minimum }
                .sortedByDescending { scores.getValue(field)[it] }
                .take(CANDIDATES_PER_FIELD)
        }

        var best: Map<TransactionField, Int>? = null
        var bestTotal = Float.NEGATIVE_INFINITY

        fun search(fieldIndex: Int, used: Set<Int>, chosen: Map<TransactionField, Int>, total: Float) {
            if (fieldIndex == CORE_FIELDS.size) {
                val complete = CORE_FIELDS.filter { it.isRequired() }.all { chosen.containsKey(it) }
                if (complete && total > bestTotal) {
                    bestTotal = total
                    best = chosen
                }
                return
            }
            val field = CORE_FIELDS[fieldIndex]
            for (column in candidates.getValue(field)) {
                if (column in used) continue
                search(
                    fieldIndex + 1,
                    used + column,
                    chosen + (field to column),
                    total + scores.getValue(field)[column],
                )
            }
            // Leaving an optional field out is a legitimate outcome, not a failure.
            if (!field.isRequired()) search(fieldIndex + 1, used, chosen, total)
        }

        search(0, emptySet(), emptyMap(), 0f)
        return best ?: emptyMap()
    }

    private fun TransactionField.isRequired() =
        this == TransactionField.DATE || this == TransactionField.AMOUNT

    /**
     * Recognizes the "Débito | Crédito" layout every bank statement uses: a
     * leftover column that also reads as money and almost never carries a
     * value on the same row as the amount column. Which of the pair is the
     * credit side comes from the headers when they say so, and otherwise from
     * the convention that credit is printed last.
     */
    private fun assignCreditColumn(
        assignment: MutableMap<TransactionField, Int>,
        features: List<ColumnFeatures>,
        dataRows: List<List<String>>,
        scores: Map<TransactionField, FloatArray>,
    ) {
        val amountColumn = assignment[TransactionField.AMOUNT] ?: return
        val taken = assignment.values.toSet()

        val creditCandidate = features
            .filter { it.index !in taken && scores.getValue(TransactionField.CREDIT_AMOUNT)[it.index] >= MIN_REQUIRED_SCORE }
            .filter { it.moneyRatio >= 0.5f }
            .filter { exclusivityWith(dataRows, amountColumn, it.index) >= MIN_EXCLUSIVITY }
            .maxByOrNull { it.moneyRatio }
            ?: return

        val amountHeader = features[amountColumn].headerField
        val candidateHeader = creditCandidate.headerField
        val creditColumn = when {
            candidateHeader == TransactionField.CREDIT_AMOUNT -> creditCandidate.index
            amountHeader == TransactionField.CREDIT_AMOUNT -> amountColumn
            else -> maxOf(amountColumn, creditCandidate.index)
        }
        val debitColumn = if (creditColumn == amountColumn) creditCandidate.index else amountColumn

        assignment[TransactionField.AMOUNT] = debitColumn
        assignment[TransactionField.CREDIT_AMOUNT] = creditColumn
    }

    /** Share of rows filling exactly one of the two columns. */
    private fun exclusivityWith(dataRows: List<List<String>>, first: Int, second: Int): Float {
        if (dataRows.isEmpty()) return 0f
        val exclusive = dataRows.count { row ->
            val a = row.getOrElse(first) { "" }.isNotBlank()
            val b = row.getOrElse(second) { "" }.isNotBlank()
            a != b
        }
        return exclusive / dataRows.size.toFloat()
    }
}
