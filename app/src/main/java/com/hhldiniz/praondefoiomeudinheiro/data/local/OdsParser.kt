package com.hhldiniz.praondefoiomeudinheiro.data.local

import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.InputStream
import java.util.zip.ZipInputStream

object OdsParser {

    private const val CONTENT_XML = "content.xml"
    private const val NS_OFFICE = "urn:oasis:names:tc:opendocument:xmlns:office:1.0"
    private const val NS_TABLE = "urn:oasis:names:tc:opendocument:xmlns:table:1.0"
    private const val NS_TEXT = "urn:oasis:names:tc:opendocument:xmlns:text:1.0"

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

    private fun parseXml(inputStream: InputStream): List<List<String>> {
        val rows = mutableListOf<List<String>>()
        var currentRow = mutableListOf<String>()
        var cellText: String? = null
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
                            columnRepeatCount = 1

                            val repeats = parser.getAttributeValue(NS_TABLE, "number-columns-repeated")
                            if (repeats != null) {
                                columnRepeatCount = repeats.toIntOrNull() ?: 1
                            }

                            val valueType = parser.getAttributeValue(NS_OFFICE, "value-type")
                            val rawValue = when (valueType) {
                                "float", "currency" -> parser.getAttributeValue(NS_OFFICE, "value")
                                "date" -> parser.getAttributeValue(NS_OFFICE, "date-value")
                                "time" -> parser.getAttributeValue(NS_OFFICE, "time-value")
                                "boolean" -> parser.getAttributeValue(NS_OFFICE, "boolean-value")
                                else -> null
                            }
                            if (rawValue != null) {
                                cellText = rawValue
                            }
                        }
                        ns == NS_TEXT && name == "p" -> {
                            insideTextP = true
                        }
                    }
                }
                XmlPullParser.TEXT -> {
                    if (insideTextP && insideCell) {
                        val text = parser.text
                        cellText = if (cellText == null) text else cellText + text
                    }
                }
                XmlPullParser.END_TAG -> {
                    val ns = parser.namespace
                    val name = parser.name
                    when {
                        ns == NS_TABLE && name == "table-cell" -> {
                            val value = cellText?.trim() ?: ""
                            for (i in 0 until columnRepeatCount) {
                                currentRow.add(value)
                            }
                            insideCell = false
                            insideTextP = false
                            cellText = null
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
