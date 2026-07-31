package com.hhldiniz.praondefoiomeudinheiro.data.local.xml

/**
 * A tiny namespace-aware XML pull parser, replacing `org.xmlpull` (Android
 * only) so `OdsParser` can live in common code. It covers exactly the subset
 * an OpenDocument `content.xml` uses: elements with namespaced attributes,
 * character data, self-closing tags, comments, CDATA sections, the XML
 * declaration and a DOCTYPE.
 *
 * The event/accessor shape mirrors `XmlPullParser` so the calling code reads
 * the same as before.
 */
class XmlPullReader(private val input: String) {

    companion object {
        const val START_DOCUMENT = 0
        const val START_TAG = 1
        const val END_TAG = 2
        const val TEXT = 3
        const val END_DOCUMENT = 4
    }

    /** Type of the event the reader is currently positioned on. */
    var eventType: Int = START_DOCUMENT
        private set

    /** Local name of the current element; empty for non-element events. */
    var name: String = ""
        private set

    /** Namespace URI of the current element; empty when the element is unprefixed. */
    var namespace: String = ""
        private set

    /** Character data of the current [TEXT] event. */
    var text: String = ""
        private set

    private var position = 0

    /** Attributes of the current start tag, keyed by "namespaceUri|localName". */
    private var attributes: Map<String, String> = emptyMap()

    /** Prefix → namespace URI, one frame per open element. */
    private val namespaceScopes = ArrayDeque<Map<String, String>>()

    /** Element stack of (namespace, localName), so end tags report the same values. */
    private val openElements = ArrayDeque<Pair<String, String>>()

    /**
     * Value of the attribute with the given [namespaceUri] (null or empty for
     * an unprefixed attribute) and local [attributeName], or null if absent.
     */
    fun getAttributeValue(namespaceUri: String?, attributeName: String): String? =
        attributes["${namespaceUri.orEmpty()}|$attributeName"]

    /**
     * Advances to the next event and returns its type. A self-closing element
     * is reported as a START_TAG followed by its matching END_TAG.
     */
    fun next(): Int {
        if (pendingSelfClose) {
            pendingSelfClose = false
            return popElement()
        }
        return advance()
    }

    private fun advance(): Int {
        if (eventType == END_DOCUMENT) return END_DOCUMENT

        while (true) {
            if (position >= input.length) {
                eventType = END_DOCUMENT
                clearElementState()
                return eventType
            }

            if (input[position] == '<') {
                when {
                    input.startsWith("<?", position) -> {
                        position = skipUntil("?>", position)
                        continue
                    }
                    input.startsWith("<!--", position) -> {
                        position = skipUntil("-->", position)
                        continue
                    }
                    input.startsWith("<![CDATA[", position) -> {
                        val end = input.indexOf("]]>", position)
                        val contentEnd = if (end < 0) input.length else end
                        text = input.substring(position + 9, contentEnd)
                        position = if (end < 0) input.length else end + 3
                        eventType = TEXT
                        return eventType
                    }
                    input.startsWith("<!", position) -> {
                        // DOCTYPE and friends: skip to the matching '>'.
                        position = skipUntil(">", position)
                        continue
                    }
                    input.startsWith("</", position) -> return readEndTag()
                    else -> return readStartTag()
                }
            }

            val nextTag = input.indexOf('<', position)
            val end = if (nextTag < 0) input.length else nextTag
            val raw = input.substring(position, end)
            position = end
            text = decodeEntities(raw)
            eventType = TEXT
            return eventType
        }
    }

    private fun readStartTag(): Int {
        val tagEnd = findTagEnd(position)
        val body = input.substring(position + 1, tagEnd).trim()
        position = tagEnd + 1

        val selfClosing = body.endsWith("/")
        val content = if (selfClosing) body.dropLast(1).trim() else body

        val qualifiedName = content.takeWhile { !it.isWhitespace() }
        val rawAttributes = parseAttributes(content.substring(qualifiedName.length))

        val declarations = mutableMapOf<String, String>()
        val inherited = namespaceScopes.lastOrNull() ?: emptyMap()
        declarations.putAll(inherited)
        rawAttributes.forEach { (attrName, value) ->
            when {
                attrName == "xmlns" -> declarations[""] = value
                attrName.startsWith("xmlns:") -> declarations[attrName.substring(6)] = value
            }
        }
        namespaceScopes.addLast(declarations)

        attributes = rawAttributes
            .filterKeys { it != "xmlns" && !it.startsWith("xmlns:") }
            .entries
            .associate { (attrName, value) ->
                val colon = attrName.indexOf(':')
                // Unprefixed attributes are in no namespace, per the XML spec.
                val key = if (colon < 0) {
                    "|$attrName"
                } else {
                    "${declarations[attrName.substring(0, colon)].orEmpty()}|${attrName.substring(colon + 1)}"
                }
                key to value
            }

        val colon = qualifiedName.indexOf(':')
        name = if (colon < 0) qualifiedName else qualifiedName.substring(colon + 1)
        namespace = if (colon < 0) declarations[""].orEmpty() else declarations[qualifiedName.substring(0, colon)].orEmpty()

        openElements.addLast(namespace to name)
        if (selfClosing) {
            // Emit START now and remember to emit the matching END next.
            pendingSelfClose = true
        }
        eventType = START_TAG
        return eventType
    }

    private var pendingSelfClose = false

    private fun readEndTag(): Int {
        val tagEnd = findTagEnd(position)
        position = tagEnd + 1
        return popElement()
    }

    private fun popElement(): Int {
        val element = openElements.removeLastOrNull()
        namespaceScopes.removeLastOrNull()
        namespace = element?.first.orEmpty()
        name = element?.second.orEmpty()
        attributes = emptyMap()
        eventType = END_TAG
        return eventType
    }

    /** Finds the '>' closing the tag starting at [start], ignoring '>' inside quoted values. */
    private fun findTagEnd(start: Int): Int {
        var i = start + 1
        var quote: Char? = null
        while (i < input.length) {
            val c = input[i]
            when {
                quote != null -> if (c == quote) quote = null
                c == '"' || c == '\'' -> quote = c
                c == '>' -> return i
            }
            i++
        }
        return input.length - 1
    }

    private fun parseAttributes(source: String): Map<String, String> {
        val result = LinkedHashMap<String, String>()
        var i = 0
        while (i < source.length) {
            while (i < source.length && source[i].isWhitespace()) i++
            if (i >= source.length) break

            val nameStart = i
            while (i < source.length && source[i] != '=' && !source[i].isWhitespace()) i++
            val attrName = source.substring(nameStart, i)
            while (i < source.length && source[i].isWhitespace()) i++
            if (i >= source.length || source[i] != '=') continue
            i++
            while (i < source.length && source[i].isWhitespace()) i++
            if (i >= source.length) break

            val quote = source[i]
            if (quote != '"' && quote != '\'') continue
            i++
            val valueStart = i
            while (i < source.length && source[i] != quote) i++
            val value = source.substring(valueStart, minOf(i, source.length))
            i++
            if (attrName.isNotEmpty()) result[attrName] = decodeEntities(value)
        }
        return result
    }

    private fun skipUntil(terminator: String, from: Int): Int {
        val index = input.indexOf(terminator, from)
        return if (index < 0) input.length else index + terminator.length
    }

    private fun clearElementState() {
        name = ""
        namespace = ""
        attributes = emptyMap()
    }

    /** Expands the five predefined entities plus numeric character references. */
    private fun decodeEntities(raw: String): String {
        if ('&' !in raw) return raw
        val builder = StringBuilder(raw.length)
        var i = 0
        while (i < raw.length) {
            val c = raw[i]
            if (c != '&') {
                builder.append(c)
                i++
                continue
            }
            val semicolon = raw.indexOf(';', i)
            if (semicolon < 0) {
                builder.append(c)
                i++
                continue
            }
            val entity = raw.substring(i + 1, semicolon)
            val replacement = when {
                entity == "amp" -> "&"
                entity == "lt" -> "<"
                entity == "gt" -> ">"
                entity == "quot" -> "\""
                entity == "apos" -> "'"
                entity.startsWith("#x") || entity.startsWith("#X") ->
                    entity.substring(2).toIntOrNull(16)?.let { codePointToString(it) }
                entity.startsWith("#") -> entity.substring(1).toIntOrNull()?.let { codePointToString(it) }
                else -> null
            }
            if (replacement == null) {
                builder.append(c)
                i++
            } else {
                builder.append(replacement)
                i = semicolon + 1
            }
        }
        return builder.toString()
    }

    private fun codePointToString(codePoint: Int): String = when {
        codePoint < 0 || codePoint > 0x10FFFF -> ""
        codePoint <= 0xFFFF -> codePoint.toChar().toString()
        else -> {
            val adjusted = codePoint - 0x10000
            charArrayOf(
                (0xD800 + (adjusted shr 10)).toChar(),
                (0xDC00 + (adjusted and 0x3FF)).toChar(),
            ).concatToString()
        }
    }
}
