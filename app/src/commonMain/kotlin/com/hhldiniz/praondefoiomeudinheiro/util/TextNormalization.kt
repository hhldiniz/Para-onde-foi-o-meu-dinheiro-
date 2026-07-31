package com.hhldiniz.praondefoiomeudinheiro.util

/**
 * Accent folding for header-name matching, replacing `java.text.Normalizer`
 * (which has no common-code equivalent). Only the Latin-1/Latin-Extended-A
 * letters that appear in the languages this app recognises are mapped;
 * anything else is passed through untouched.
 */
private val ACCENT_FOLDING: Map<Char, Char> = buildMap {
    fun map(accented: String, plain: Char) = accented.forEach { put(it, plain) }
    map("àáâãäåāăą", 'a')
    map("çćĉċč", 'c')
    map("ďđ", 'd')
    map("èéêëēĕėęě", 'e')
    map("ĝğġģ", 'g')
    map("ĥħ", 'h')
    map("ìíîïĩīĭįı", 'i')
    map("ĵ", 'j')
    map("ķ", 'k')
    map("ĺļľłŀ", 'l')
    map("ñńņňŉ", 'n')
    map("òóôõöøōŏő", 'o')
    map("ŕŗř", 'r')
    map("śŝşš", 's')
    map("ţťŧ", 't')
    map("ùúûüũūŭůűų", 'u')
    map("ŵ", 'w')
    map("ýÿŷ", 'y')
    map("źżž", 'z')
}

/**
 * Lower-cases [text], trims it and strips diacritics, so "Descrição",
 * "DESCRICAO" and "descricao" all compare equal.
 */
fun normalizeForMatching(text: String): String {
    val lowercased = text.trim().lowercase()
    if (lowercased.none { it in ACCENT_FOLDING }) return lowercased
    return buildString(lowercased.length) {
        lowercased.forEach { append(ACCENT_FOLDING[it] ?: it) }
    }
}
