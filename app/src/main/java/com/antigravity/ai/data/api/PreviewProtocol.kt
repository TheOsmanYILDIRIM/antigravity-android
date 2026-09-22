package com.antigravity.ai.data.api

import com.google.gson.JsonParser

const val PREVIEW_ORIGIN = "http://127.0.0.1:8081"

fun absolutePreviewUrl(raw: String, origin: String = PREVIEW_ORIGIN): String {
    if (raw.startsWith("http://") || raw.startsWith("https://")) return raw
    return origin.trimEnd('/') + "/" + raw.trimStart('/')
}

data class InspectorPayload(val selector: String, val outerHTML: String, val bounds: String, val styles: String)

fun parseInspectorMessage(raw: String, maxOuterHtml: Int = 8192): InspectorPayload? = runCatching {
    val o = JsonParser.parseString(raw).asJsonObject
    val selector = o.get("selector")?.asString.orEmpty()
    require(selector.isNotBlank())
    InspectorPayload(selector, o.get("outerHTML")?.asString.orEmpty().take(maxOuterHtml), o.get("bounds")?.asString.orEmpty(), o.get("styles")?.asString.orEmpty())
}.getOrNull()
