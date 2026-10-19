package com.vasudev.ai.master.data.remote

import com.vasudev.ai.master.core.SearchResult
import com.vasudev.ai.master.data.SecurePrefs
import com.vasudev.ai.master.core.Config
import com.vasudev.ai.master.core.stripHtml
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.isSuccess
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Web search grounding. Bing Web Search API was retired by Microsoft in Aug 2025,
 * so Brave Search is the default provider. Swap implementations as needed.
 */
@Singleton
class SearchApi @Inject constructor(
    private val client: HttpClient,
    private val json: Json,
    private val prefs: SecurePrefs
) {

    suspend fun search(query: String, count: Int = 5): List<SearchResult> {
        val key = prefs.searchApiKey
        if (key.isBlank()) return emptyList()
        return try {
            val response = client.get(Config.SEARCH_ENDPOINT) {
                parameter("q", query)
                parameter("count", count)
                parameter("country", "in")
                header("X-Subscription-Token", key)
                header(HttpHeaders.Accept, "application/json")
            }
            if (!response.status.isSuccess()) return emptyList()
            val root = json.parseToJsonElement(response.bodyAsText()).jsonObject
            root["web"]
                ?.jsonObject
                ?.get("results")
                ?.jsonArray
                ?.mapNotNull { element ->
                    val obj = element.jsonObject
                    val title = obj["title"]?.jsonPrimitive?.contentOrNull
                    val url = obj["url"]?.jsonPrimitive?.contentOrNull
                    val desc = obj["description"]?.jsonPrimitive?.contentOrNull
                    if (title != null && url != null) {
                        SearchResult(
                            title = title,
                            url = url,
                            snippet = desc.orEmpty().stripHtml()
                        )
                    } else {
                        null
                    }
                }
                .orEmpty()
        } catch (e: Exception) {
            emptyList()
        }
    }
}
