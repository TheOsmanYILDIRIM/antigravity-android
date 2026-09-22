package com.antigravity.ai.data.api

import org.junit.Assert.*
import org.junit.Test

class PreviewProtocolTest {
    @Test fun relativePreviewUrlsBecomeLocalhostAbsoluteUrls() {
        assertEquals("http://127.0.0.1:8081/preview/a%20b", absolutePreviewUrl("/preview/a%20b"))
        assertEquals("http://127.0.0.1:8081/preview/site%20%C3%B6nizleme.html", absolutePreviewUrl("http://127.0.0.1:8081/preview/site%20%C3%B6nizleme.html"))
        assertEquals("https://example.test/x", absolutePreviewUrl("https://example.test/x"))
        assertEquals("http://127.0.0.1:8081/%C3%B6n%20izleme.html", absolutePreviewUrl("/%C3%B6n%20izleme.html"))
        assertEquals("http://127.0.0.1:8081/", absolutePreviewUrl(""))
    }

    @Test fun inspectorMessagesAreSafeAndOuterHtmlIsBounded() {
        val payload = parseInspectorMessage("{\"selector\":\"#app\",\"outerHTML\":\"${"x".repeat(9000)}\",\"bounds\":\"{}\",\"styles\":\"{}\"}")
        assertNotNull(payload)
        assertEquals("#app", payload!!.selector)
        assertEquals("{}", payload.bounds)
        assertEquals("{}", payload.styles)
        assertEquals(8192, payload!!.outerHTML.length)
        assertNull(parseInspectorMessage("not-json"))
        assertNull(parseInspectorMessage("{}"))
    }
}
