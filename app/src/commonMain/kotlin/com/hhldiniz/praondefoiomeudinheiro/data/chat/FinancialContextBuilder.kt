package com.hhldiniz.praondefoiomeudinheiro.data.chat

import com.hhldiniz.praondefoiomeudinheiro.data.local.entity.ImportedEntry
import com.hhldiniz.praondefoiomeudinheiro.data.local.entity.Investment
import com.hhldiniz.praondefoiomeudinheiro.domain.model.CurrencyOption
import com.hhldiniz.praondefoiomeudinheiro.domain.model.YieldMode
import com.hhldiniz.praondefoiomeudinheiro.domain.model.formatYieldRate
import com.hhldiniz.praondefoiomeudinheiro.util.formatDayMonthYear
import com.hhldiniz.praondefoiomeudinheiro.util.localDateOf
import kotlin.math.abs
import kotlin.math.roundToLong

/** Everything the assistant is told about the user's money, read once per question. */
data class FinancialSnapshot(
    val currency: CurrencyOption,
    /** The starting net worth typed during onboarding. */
    val patrimony: Double,
    val entries: List<ImportedEntry>,
    val investments: List<Investment>,
)

/**
 * Turns the user's data into the short, plain-text briefing the assistant
 * reads before every answer — which is what lets a 1.5B model running on a
 * phone answer "where did my money go?" about *this* user's money.
 *
 * A small model has a small context window, so this is a summary rather than
 * a dump: totals, the last months side by side, where the expenses
 * concentrate, the largest and the latest transactions and the investment
 * positions. Every figure is computed here, in code, so the model only has to
 * read numbers, never to add them up — small models are poor at arithmetic and
 * good at reading.
 *
 * The labels are in English, the language the system prompt is written in;
 * the model answers in whichever language the user writes.
 */
object FinancialContextBuilder {

    private const val MONTHS_IN_TABLE = 6
    private const val RECENT_MONTHS = 3
    private const val TOP_CATEGORIES = 8
    private const val TOP_EXPENSES = 5
    private const val LATEST_ENTRIES = 10
    private const val MAX_INVESTMENTS = 10
    private const val MAX_DESCRIPTION = 40

    fun describe(snapshot: FinancialSnapshot): String = buildString {
        val money = MoneyFormat(snapshot.currency)
        val entries = snapshot.entries.sortedByDescending { it.dateMillis }

        appendLine("Currency: ${snapshot.currency.code} (${snapshot.currency.symbol}).")
        appendLine("Starting net worth typed by the user: ${money(snapshot.patrimony)}.")

        if (entries.isEmpty()) {
            appendLine("The user has not recorded any income or expense yet.")
        } else {
            describeEntries(entries, snapshot.patrimony, money)
        }

        appendLine()
        describeInvestments(snapshot.investments, money)
    }.trimEnd()

    private fun StringBuilder.describeEntries(entries: List<ImportedEntry>, patrimony: Double, money: MoneyFormat) {
        val income = entries.filter { !it.isExpense }.sumOf { it.amount }
        val expenses = entries.filter { it.isExpense }.sumOf { it.amount }

        appendLine(
            "Records: ${entries.size} transactions from ${formatDayMonthYear(entries.last().dateMillis)} " +
                "to ${formatDayMonthYear(entries.first().dateMillis)}."
        )
        appendLine(
            "All recorded: income ${money(income)}, expenses ${money(expenses)}, " +
                "difference ${money(income - expenses)}."
        )
        appendLine("Current balance (starting net worth + income - expenses): ${money(patrimony + income - expenses)}.")

        // Months are listed most recent first, and only the ones with data —
        // a statement imported months ago would otherwise show up as a run of
        // empty months that says nothing.
        val byMonth = entries.groupBy { monthKey(it.dateMillis) }
        val months = byMonth.keys.sortedDescending()
        appendLine()
        appendLine("Monthly totals (most recent first):")
        for (month in months.take(MONTHS_IN_TABLE)) {
            val inMonth = byMonth.getValue(month)
            val monthIncome = inMonth.filter { !it.isExpense }.sumOf { it.amount }
            val monthExpenses = inMonth.filter { it.isExpense }.sumOf { it.amount }
            appendLine(
                "- $month: income ${money(monthIncome)}, expenses ${money(monthExpenses)}, " +
                    "difference ${money(monthIncome - monthExpenses)}"
            )
        }

        val recentMonths = months.take(RECENT_MONTHS).toSet()
        val recent = entries.filter { monthKey(it.dateMillis) in recentMonths }
        val recentLabel = recentMonths.sorted().let { if (it.size == 1) it.first() else "${it.first()} to ${it.last()}" }

        appendCategoryBreakdown("Expenses by category, $recentLabel", recent.filter { it.isExpense }, money)
        appendCategoryBreakdown("Expenses by category, all recorded", entries.filter { it.isExpense }, money)
        appendCategoryBreakdown("Income by category, all recorded", entries.filter { !it.isExpense }, money)

        val largest = recent.filter { it.isExpense }.sortedByDescending { it.amount }.take(TOP_EXPENSES)
        if (largest.isNotEmpty()) {
            appendLine()
            appendLine("Largest expenses, $recentLabel:")
            largest.forEach { appendLine("- ${describeEntry(it, money)}") }
        }

        appendLine()
        appendLine("Latest transactions:")
        entries.take(LATEST_ENTRIES).forEach { appendLine("- ${describeEntry(it, money)}") }
    }

    private fun StringBuilder.appendCategoryBreakdown(title: String, entries: List<ImportedEntry>, money: MoneyFormat) {
        if (entries.isEmpty()) return
        val total = entries.sumOf { it.amount }
        val byCategory = entries
            .groupBy { it.category.ifBlank { "Uncategorized" } }
            .mapValues { (_, list) -> list.sumOf { it.amount } }
            .entries
            .sortedByDescending { it.value }

        appendLine()
        appendLine("$title (total ${money(total)}):")
        for ((category, amount) in byCategory.take(TOP_CATEGORIES)) {
            appendLine("- ${clean(category)}: ${money(amount)} (${percent(amount, total)})")
        }
        val rest = byCategory.drop(TOP_CATEGORIES)
        if (rest.isNotEmpty()) {
            val restTotal = rest.sumOf { it.value }
            appendLine("- ${rest.size} other categories: ${money(restTotal)} (${percent(restTotal, total)})")
        }
    }

    private fun StringBuilder.describeInvestments(investments: List<Investment>, money: MoneyFormat) {
        if (investments.isEmpty()) {
            appendLine("Investments: none recorded.")
            return
        }
        val invested = investments.sumOf { it.investedAmount }
        val current = investments.sumOf { it.currentValue }
        appendLine(
            "Investments (values typed by the user, not live quotes): ${investments.size} positions, " +
                "invested ${money(invested)}, current value ${money(current)}, " +
                "result ${money(current - invested)} (${signedPercent(current - invested, invested)})."
        )
        for (position in investments.sortedByDescending { it.currentValue }.take(MAX_INVESTMENTS)) {
            val details = buildList {
                add(position.type.key)
                if (position.institution.isNotBlank()) add(clean(position.institution))
                if (position.hasYield) add("contracted ${describeYield(position.yieldMode, position.yieldRate!!)}")
            }.joinToString(", ")
            appendLine(
                "- ${clean(position.name)} ($details): invested ${money(position.investedAmount)}, " +
                    "now ${money(position.currentValue)}, " +
                    "result ${signedPercent(position.profit, position.investedAmount)}"
            )
        }
        if (investments.size > MAX_INVESTMENTS) {
            appendLine("- and ${investments.size - MAX_INVESTMENTS} smaller positions")
        }
    }

    private fun describeEntry(entry: ImportedEntry, money: MoneyFormat): String {
        val kind = if (entry.isExpense) "expense" else "income"
        val parts = buildList {
            add(formatDayMonthYear(entry.dateMillis))
            add(kind)
            add(money(entry.amount))
            if (entry.category.isNotBlank()) add(clean(entry.category))
            if (entry.description.isNotBlank()) add(clean(entry.description).take(MAX_DESCRIPTION))
        }
        return parts.joinToString(" | ")
    }

    private fun describeYield(mode: YieldMode, rate: Double): String {
        val value = formatYieldRate(rate)
        return when (mode) {
            YieldMode.NONE -> value
            YieldMode.PREFIXED -> "$value per year"
            YieldMode.CDI_PERCENT -> "$value of CDI"
            YieldMode.CDI_PLUS -> "CDI + $value"
            YieldMode.IPCA_PLUS -> "IPCA + $value"
            YieldMode.IGPM_PLUS -> "IGP-M + $value"
            YieldMode.SELIC_PLUS -> "Selic + $value"
        }
    }

    /** `yyyy-MM` of [epochMillis] in the device's time zone; sorts chronologically as text. */
    private fun monthKey(epochMillis: Long): String {
        val date = localDateOf(epochMillis)
        return "${date.year.toString().padStart(4, '0')}-${date.month.toString().padStart(2, '0')}"
    }

    private fun percent(part: Double, total: Double): String =
        if (total == 0.0) "0%" else "${(part / total * 100).roundToLong()}%"

    private fun signedPercent(part: Double, total: Double): String {
        if (total == 0.0) return "n/a"
        val value = (part / total * 1000).roundToLong() / 10.0
        return if (value > 0) "+$value%" else "$value%"
    }

    /** One line, no template markers: descriptions come straight from imported files. */
    private fun clean(text: String): String =
        ChatPromptBuilder.sanitize(text).replace('\n', ' ').replace('\r', ' ').trim()

    /**
     * Amounts as `R$ 1234.56`: a fixed, locale-free form, since the point is
     * for the model to read the number unambiguously, not for a person to
     * read it in their locale's convention.
     */
    private class MoneyFormat(private val currency: CurrencyOption) {
        operator fun invoke(value: Double): String {
            val cents = (abs(value) * 100).roundToLong()
            val text = "${currency.symbol} ${cents / 100}.${(cents % 100).toString().padStart(2, '0')}"
            return if (value < 0 && cents != 0L) "-$text" else text
        }
    }
}
