package com.hhldiniz.praondefoiomeudinheiro.data.local

import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.InputStream
import java.util.zip.ZipInputStream

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
     * Opens the ZIP stream, locates content.xml and delegates to [parseXml].
     * Returns an empty list if the content file is not found.
     */
    fun parse(inputStream: InputStream): List<List<String>> {
        val zipStream = ZipInputStream(inputStream)
        var entry = zipStream.nextEntry
        while (entry != null) {
            if (entry.name == CONTENT_XML) {
                val rows = parseXml(zipStream)
                zipStream.closeEntry()
                zipStream.close()
                return rows
            }
            zipStream.closeEntry()
            entry = zipStream.nextEntry
        }
        zipStream.close()
        return emptyList()
    }

    /** Parses the content.xml stream using XmlPullParser, extracting table rows and cell values. */
    private fun parseXml(inputStream: InputStream): List<List<String>> {
        val rows = mutableListOf<List<String>>()
        var currentRow = mutableListOf<String>()
        var cellText: String? = null
        var cellAttrValue: String? = null
        var cellAttrCurrency: String? = null
        var insideCell = false
        var insideTextP = false
        var columnRepeatCount = 1

        val factory = XmlPullParserFactory.newInstance()
        factory.isNamespaceAware = true
        val parser = factory.newPullParser()
        parser.setInput(inputStream, "UTF-8")

        var eventType = parser.eventType
        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
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
                                columnRepeatCount = repeats.toIntOrNull() ?: 1
                            }

                            val valueType = parser.getAttributeValue(NS_OFFICE, "value-type")
                            when (valueType) {
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
                XmlPullParser.TEXT -> {
                    if (insideTextP && insideCell) {
                        cellText = parser.text
                    }
                }
                XmlPullParser.END_TAG -> {
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
