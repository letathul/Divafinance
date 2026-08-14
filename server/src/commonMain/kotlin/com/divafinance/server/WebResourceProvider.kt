package com.divafinance.server

import io.ktor.http.ContentType

object WebResourceProvider {
    fun loadResources(): Map<String, Pair<String, ContentType>> {
        return mapOf(
            "index.html" to Pair(INDEX_HTML, ContentType.Text.Html),
            "style.css" to Pair(STYLE_CSS, ContentType.Text.CSS),
            "app.js" to Pair(APP_JS, ContentType.Application.JavaScript),
        )
    }
}
