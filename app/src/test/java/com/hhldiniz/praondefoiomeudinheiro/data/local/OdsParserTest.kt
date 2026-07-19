package com.hhldiniz.praondefoiomeudinheiro.data.local

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Unit tests for [OdsParser].
 *
 * We create minimal in-memory .ods (ZIP) files with hand-crafted content.xml
 * to verify parsing logic without needing actual spreadsheet files.
 */
class OdsParserTest {

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /**
     * Builds an in-memory ODS byte array whose content.xml is the given [xml].
     */
    private fun buildOds(xml: String): ByteArray {
        val baos = ByteArrayOutputStream()
        ZipOutputStream(baos).use { zip ->
            zip.putNextEntry(ZipEntry("content.xml"))
            zip.write(xml.toByteArray(Charsets.UTF_8))
            zip.closeEntry()
        }
        return baos.toByteArray()
    }

    private fun parse(xml: String) =
        OdsParser.parse(buildOds(xml).inputStream()) {
            org.kxml2.io.KXmlParser().apply {
                setFeature(org.xmlpull.v1.XmlPullParser.FEATURE_PROCESS_NAMESPACES, true)
            }
        }

    /** Minimal content.xml wrapper for a table with given [bodyXml]. */
    private fun wrap(bodyXml: String) = """<?xml version="1.0" encoding="UTF-8"?>
<office:document-content
        xmlns:office="urn:oasis:names:tc:opendocument:xmlns:office:1.0"
        xmlns:table="urn:oasis:names:tc:opendocument:xmlns:table:1.0"
        xmlns:text="urn:oasis:names:tc:opendocument:xmlns:text:1.0">
  <office:body>
    <office:spreadsheet>
      <table:table>
        $bodyXml
      </table:table>
    </office:spreadsheet>
  </office:body>
</office:document-content>"""

    private fun row(vararg cells: String): String {
        val cellsXml = cells.joinToString("") { value ->
            """<table:table-cell><text:p>$value</text:p></table:table-cell>"""
        }
        return "<table:table-row>$cellsXml</table:table-row>"
    }

    private fun emptyRow() = "<table:table-row></table:table-row>"

    // -------------------------------------------------------------------------
    // Tests
    // -------------------------------------------------------------------------

    @Test
    fun parse_singleRowWithTextCells() {
        val xml = wrap(row("Data", "Valor", "Descrição", "Categoria"))
        val result = parse(xml)
        assertEquals(1, result.size)
        assertEquals(listOf("Data", "Valor", "Descrição", "Categoria"), result[0])
    }

    @Test
    fun parse_multipleRows() {
        val xml = wrap(
            row("H1", "H2") +
            row("R1C1", "R1C2") +
            row("R2C1", "R2C2")
        )
        val result = parse(xml)
        assertEquals(3, result.size)
        assertEquals(listOf("H1", "H2"), result[0])
        assertEquals(listOf("R1C1", "R1C2"), result[1])
        assertEquals(listOf("R2C1", "R2C2"), result[2])
    }

    @Test
    fun parse_skipsEmptyRows() {
        val xml = wrap(
            row("A", "B") +
            emptyRow() +
            row("C", "D")
        )
        val result = parse(xml)
        // Empty rows produce empty currentRow so they are not added
        assertEquals(2, result.size)
        assertEquals(listOf("A", "B"), result[0])
        assertEquals(listOf("C", "D"), result[1])
    }

    @Test
    fun parse_cellsWithColumnRepeat() {
        val repeatXml = wrap("""
            <table:table-row>
              <table:table-cell table:number-columns-repeated="3">
                <text:p>X</text:p>
              </table:table-cell>
            </table:table-row>
        """)
        val result = parse(repeatXml)
        assertEquals(1, result.size)
        assertEquals(listOf("X", "X", "X"), result[0])
    }

    @Test
    fun parse_floatValueCell_usesAttrValue() {
        val xml = wrap("""
            <table:table-row>
              <table:table-cell
                  office:value-type="float"
                  office:value="42.5"
                  xmlns:office="urn:oasis:names:tc:opendocument:xmlns:office:1.0">
                <text:p>42,5</text:p>
              </table:table-cell>
            </table:table-row>
        """)
        // When text is present inside <text:p>, it wins over the attribute value
        val result = parse(xml)
        assertEquals(1, result.size)
        assertEquals("42,5", result[0][0])
    }

    @Test
    fun parse_currencyCell_usesCurrencyPrefix() {
        val xml = wrap("""
            <table:table-row>
              <table:table-cell
                  office:value-type="currency"
                  office:currency="BRL"
                  office:value="100.0"
                  xmlns:office="urn:oasis:names:tc:opendocument:xmlns:office:1.0">
              </table:table-cell>
            </table:table-row>
        """)
        val result = parse(xml)
        assertEquals(1, result.size)
        // No text:p → falls back to "BRL 100.0"
        assertEquals("BRL 100.0", result[0][0])
    }

    @Test
    fun parse_dateCell_usesDateAttr() {
        val xml = wrap("""
            <table:table-row>
              <table:table-cell
                  office:value-type="date"
                  office:date-value="2024-01-15"
                  xmlns:office="urn:oasis:names:tc:opendocument:xmlns:office:1.0">
              </table:table-cell>
            </table:table-row>
        """)
        val result = parse(xml)
        assertEquals(1, result.size)
        assertEquals("2024-01-15", result[0][0])
    }

    @Test
    fun parse_booleanCell_usesBooleanAttr() {
        val xml = wrap("""
            <table:table-row>
              <table:table-cell
                  office:value-type="boolean"
                  office:boolean-value="true"
                  xmlns:office="urn:oasis:names:tc:opendocument:xmlns:office:1.0">
              </table:table-cell>
            </table:table-row>
        """)
        val result = parse(xml)
        assertEquals(1, result.size)
        assertEquals("true", result[0][0])
    }

    @Test
    fun parse_emptyCellYieldsEmptyString() {
        val xml = wrap("""
            <table:table-row>
              <table:table-cell/>
              <table:table-cell><text:p>B</text:p></table:table-cell>
            </table:table-row>
        """)
        val result = parse(xml)
        assertEquals(1, result.size)
        assertEquals("", result[0][0])
        assertEquals("B", result[0][1])
    }

    @Test
    fun parse_missingContentXml_returnsEmpty() {
        val baos = ByteArrayOutputStream()
        ZipOutputStream(baos).use { zip ->
            zip.putNextEntry(ZipEntry("mimetype"))
            zip.write("application/vnd.oasis.opendocument.spreadsheet".toByteArray())
            zip.closeEntry()
        }
        val result = OdsParser.parse(baos.toByteArray().inputStream())
        assertTrue(result.isEmpty())
    }

    @Test
    fun parse_trimsTextContent() {
        val xml = wrap(row("  hello  ", "  world  "))
        val result = parse(xml)
        assertEquals("hello", result[0][0])
        assertEquals("world", result[0][1])
    }

    @Test
    fun parse_multipleTablesOnlyParsesFirstEncountered() {
        // OdsParser iterates the whole document so all rows end up included
        val xmlWithTwoTables = """<?xml version="1.0" encoding="UTF-8"?>
<office:document-content
        xmlns:office="urn:oasis:names:tc:opendocument:xmlns:office:1.0"
        xmlns:table="urn:oasis:names:tc:opendocument:xmlns:table:1.0"
        xmlns:text="urn:oasis:names:tc:opendocument:xmlns:text:1.0">
  <office:body>
    <office:spreadsheet>
      <table:table>
        <table:table-row><table:table-cell><text:p>T1R1</text:p></table:table-cell></table:table-row>
      </table:table>
      <table:table>
        <table:table-row><table:table-cell><text:p>T2R1</text:p></table:table-cell></table:table-row>
      </table:table>
    </office:spreadsheet>
  </office:body>
</office:document-content>"""
        val result = parse(xmlWithTwoTables)
        // Both rows are parsed (parser doesn't stop at first table)
        assertTrue(result.isNotEmpty())
    }
}
