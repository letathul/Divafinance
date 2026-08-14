package com.divafinance.server.routing

import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.get

fun Route.staticRoutes(webResources: Map<String, Pair<String, ContentType>>) {
    get("/") {
        val (content, contentType) = webResources["index.html"]
            ?: return@get call.respond(HttpStatusCode.NotFound)
        call.respondText(content, contentType)
    }

    get("/style.css") {
        val (content, contentType) = webResources["style.css"]
            ?: return@get call.respond(HttpStatusCode.NotFound)
        call.respondText(content, contentType)
    }

    get("/app.js") {
        val (content, contentType) = webResources["app.js"]
            ?: return@get call.respond(HttpStatusCode.NotFound)
        call.respondText(content, contentType)
    }
}
