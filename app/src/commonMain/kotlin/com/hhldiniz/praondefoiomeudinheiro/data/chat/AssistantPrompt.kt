package com.hhldiniz.praondefoiomeudinheiro.data.chat

/**
 * The assistant's standing instructions, followed by the user's financial
 * briefing from [FinancialContextBuilder].
 *
 * Written in English, which small instruction-tuned models follow most
 * reliably, with an explicit rule to answer in the user's language — and the
 * app's language as the fallback for a message too short to tell ("oi").
 */
object AssistantPrompt {

    fun build(financialData: String, today: String, languageTag: String): String {
        // Interpolating the multi-line data before trimIndent() would make its
        // unindented lines the common indent and leave the template indented,
        // so the instructions are trimmed first and the data appended after.
        val instructions = """
            You are the assistant inside "Pra onde foi o meu dinheiro", a personal finance app. You run entirely on the user's device: nothing they write leaves it.
            Help the user understand their income, expenses and investments using the data below, and give practical, prudent suggestions.
            Rules:
            - Answer in the same language as the user's last message. If that is unclear, answer in ${languageName(languageTag)}.
            - Only use figures that appear in the data below and never invent numbers. If the data does not answer the question, say so and suggest what the user could record or import.
            - Be concise: short paragraphs or short lists.
            - You are not a licensed financial advisor. For big decisions (loans, large investments, taxes), suggest confirming with a professional.
            Today is $today.
        """.trimIndent()
        return "$instructions\n\nUSER'S FINANCIAL DATA\n${financialData.trim()}"
    }

    /** The name of the app language, as the model understands it best (in English). */
    fun languageName(languageTag: String): String = when (languageTag.substringBefore('-').substringBefore('_').lowercase()) {
        "pt" -> "Brazilian Portuguese"
        "es" -> "Spanish"
        "en" -> "English"
        // The app's default (untranslated) strings are Portuguese.
        else -> "Brazilian Portuguese"
    }
}
