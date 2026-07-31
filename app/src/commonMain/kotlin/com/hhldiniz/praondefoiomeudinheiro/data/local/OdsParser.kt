package com.hhldiniz.praondefoiomeudinheiro.data.local

import com.hhldiniz.praondefoiomeudinheiro.data.local.xml.XmlPullReader
import com.hhldiniz.praondefoiomeudinheiro.data.local.zip.ZipReader

/**
 * Parser for OpenDocument Spreadsheet (.ods) files. Extracts the content.xml
 * entry from the ZIP container and reads table cell values using XML pull
 * parsing.
 */
object OdsParser {

    private const val CONTENT_XML = "content.xml"
    private const val NS_OFFICE = "urn:oasis:names:tc:opendocument:xmlns:office:1.0"
    private const val NS_TABLE = "urn:oasis:names:tc:opendocument:xmlns:table:1.0"
    private const val NS_TEXT = "urn:oasis:names:tc:opendocument:xmlns:text:1.0"

    /**
     * Upper bound on how many times a single cell is physically expanded when it
     * carries a `number-columns-repeated` attribute. ODS writers (e.g. LibreOffice)
     * commonly pad trailing blank cells out to the sheet's full column count
     * (1024, 16384, ...); expanding that literally would allocate tens of
     * thousands of empty-string entries per row for no benefit, since no caller
     * reads past a handful of columns.
     */
    private const val MAX_CELL_REPEAT = 64

    /**
     * Locates content.xml inside the ODS (ZIP) container and delegates to
     * [parseXml]. Returns an empty list if the content file is not found.
     */
    fun parse(bytes: ByteArray): List<List<String>> {
        val content = ZipReader.readEntry(bytes, CONTENT_XML) ?: return emptyList()
        return parseXml(content.decodeToString())
    }

    /** Parses the content.xml document, extracting table rows and cell values. */
    private fun parseXml(xml: String): List<List<String>> {
        val rows = mutableListOf<List<String>>()
        var currentRow = mutableListOf<String>()
        var cellText: String? = null
        var cellAttrValue: String? = null
        var cellAttrCurrency: String? = null
        var insideCell = false
        var insideTextP = false
        var columnRepeatCount = 1

        val parser = XmlPullReader(xml)

        var eventType = parser.eventType
        while (eventType != XmlPullReader.END_DOCUMENT) {
            when (eventType) {
                XmlPullReader.START_TAG -> {
                    val ns = parser.namespace
                    val name = parser.name
                    when {
                        ns == NS_TABLE && name == "table-row" -> {
                            currentRow = mutableListOf()
                        }
                        ns == NS_TABLE && name == "table-cell" -> {
                            insideCell = true
                            cellText = null
                            cellAttrValue = null
                            cellAttrCurrency = null
                            columnRepeatCount = 1

                            val repeats = parser.getAttributeValue(NS_TABLE, "number-columns-repeated")
                            if (repeats != null) {
                                columnRepeatCount = (repeats.toIntOrNull() ?: 1).coerceAtMost(MAX_CELL_REPEAT)
                            }

                            when (parser.getAttributeValue(NS_OFFICE, "value-type")) {
                                "float" -> cellAttrValue = parser.getAttributeValue(NS_OFFICE, "value")
                                "currency" -> {
                                    cellAttrValue = parser.getAttributeValue(NS_OFFICE, "value")
                                    cellAttrCurrency = parser.getAttributeValue(NS_OFFICE, "currency")
                                }
                                "date" -> cellAttrValue = parser.getAttributeValue(NS_OFFICE, "date-value")
                                "time" -> cellAttrValue = parser.getAttributeValue(NS_OFFICE, "time-value")
                                "boolean" -> cellAttrValue = parser.getAttributeValue(NS_OFFICE, "boolean-value")
                            }
                        }
                        ns == NS_TEXT && name == "p" -> {
                            insideTextP = true
                        }
                    }
                }
                XmlPullReader.TEXT -> {
                    if (insideTextP && insideCell) {
                        cellText = parser.text
                    }
                }
                XmlPullReader.END_TAG -> {
                    val ns = parser.namespace
                    val name = parser.name
                    when {
                        ns == NS_TABLE && name == "table-cell" -> {
                            val value = when {
                                cellText != null -> cellText.trim()
                                cellAttrCurrency != null -> "$cellAttrCurrency ${cellAttrValue ?: ""}"
                                cellAttrValue != null -> cellAttrValue
                                else -> ""
                            }
                            for (i in 0 until columnRepeatCount) {
                                currentRow.add(value)
                            }
                            insideCell = false
                            insideTextP = false
                            cellText = null
                            cellAttrValue = null
                            cellAttrCurrency = null
                            columnRepeatCount = 1
                        }
                        ns == NS_TEXT && name == "p" -> {
                            insideTextP = false
                        }
                        ns == NS_TABLE && name == "table-row" -> {
                            if (currentRow.isNotEmpty()) {
                                rows.add(currentRow.toList())
                            }
                        }
                    }
                }
            }
            eventType = parser.next()
        }

        return rows
    }
}
