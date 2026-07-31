package com.hhldiniz.praondefoiomeudinheiro.data.local.xml

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Covers the hand-rolled XML reader that replaced `org.xmlpull` in common code. */
class XmlPullReaderTest {

    private fun events(xml: String): List<String> {
        val reader = XmlPullReader(xml)
        val result = mutableListOf<String>()
        var event = reader.next()
        while (event != XmlPullReader.END_DOCUMENT) {
            when (event) {
                XmlPullReader.START_TAG -> result.add("start:${reader.namespace}|${reader.name}")
                XmlPullReader.END_TAG -> result.add("end:${reader.namespace}|${reader.name}")
                XmlPullReader.TEXT -> if (reader.text.isNotBlank()) result.add("text:${reader.text}")
            }
            event = reader.next()
        }
        return result
    }

    @Test
    fun readsElementsAndText() {
        assertEquals(
            listOf("start:|a", "start:|b", "text:hi", "end:|b", "end:|a"),
            events("<a><b>hi</b></a>"),
        )
    }

    @Test
    fun resolvesPrefixedNamespaces() {
        assertEquals(
            listOf("start:urn:x|root", "start:urn:x|child", "end:urn:x|child", "end:urn:x|root"),
            events("""<t:root xmlns:t="urn:x"><t:child/></t:root>"""),
        )
    }

    @Test
    fun selfClosingTagEmitsStartAndEnd() {
        assertEquals(listOf("start:|a", "start:|b", "end:|b", "end:|a"), events("<a><b/></a>"))
    }

    @Test
    fun readsNamespacedAttributes() {
        val reader = XmlPullReader("""<r xmlns:t="urn:x"><c t:v="7" plain="8"/></r>""")
        reader.next() // <r>
        reader.next() // <c>
        assertEquals("7", reader.getAttributeValue("urn:x", "v"))
        assertEquals("8", reader.getAttributeValue(null, "plain"))
        assertNull(reader.getAttributeValue("urn:x", "missing"))
        // An unprefixed attribute is in no namespace, so it must not be found under one.
        assertNull(reader.getAttributeValue("urn:x", "plain"))
    }

    @Test
    fun skipsDeclarationCommentsAndDoctype() {
        assertEquals(
            listOf("start:|a", "text:x", "end:|a"),
            events("""<?xml version="1.0"?><!DOCTYPE a><!-- note --><a>x</a>"""),
        )
    }

    @Test
    fun decodesEntitiesAndCharacterReferences() {
        val reader = XmlPullReader("""<a t="x &amp; y">1 &lt; 2 &#65;</a>""")
        reader.next()
        assertEquals("x & y", reader.getAttributeValue(null, "t"))
        assertEquals(XmlPullReader.TEXT, reader.next())
        assertEquals("1 < 2 A", reader.text)
    }

    @Test
    fun readsCdataAsText() {
        assertEquals(listOf("start:|a", "text:a < b", "end:|a"), events("<a><![CDATA[a < b]]></a>"))
    }

    @Test
    fun defaultNamespaceAppliesToUnprefixedElements() {
        assertEquals(
            listOf("start:urn:d|a", "start:urn:d|b", "end:urn:d|b", "end:urn:d|a"),
            events("""<a xmlns="urn:d"><b/></a>"""),
        )
    }
}
