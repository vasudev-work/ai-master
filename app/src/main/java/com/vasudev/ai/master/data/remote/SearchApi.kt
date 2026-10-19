package com.vasudev.ai.master.data.remote

import com.vasudev.ai.master.core.Config
import com.vasudev.ai.master.core.SearchResult
import com.vasudev.ai.master.core.stripHtml
import com.vasudev.ai.master.data.SecurePrefs
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
import java.net.URLDecoder
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Multi-source web search. Results from every source are merged and de-duplicated.
 *
 *  - **Wikipedia**  — official, keyless API. Always on. Uses the user's language
 *                     when it is an Indian language (hi, mr, bn, ta, te).
 *  - **DuckDuckGo** — keyless HTML endpoint (best effort; may be rate-limited).
 *  - **Reddit**     — public `.json` search endpoint (community discussions).
 *  - **Brave**      — used only if the user saved a Brave Search API key.
 *
 * Note on iBharat.org and Qmamu: neither offers a public search API, so the app
 * does not call them directly. (Qmamu in particular serves Google Programmable
 * Search results under the hood, so it is not an independent index.)
 */
@Singleton
class SearchApi @Inject constructor(
    private val client: HttpClient,
    private val json: Json,
    private val prefs: SecurePrefs
) {

    suspend fun search(query: String, languageTag: String = "en-IN", count: Int = 8): List<SearchResult> {
        val out = mutableListOf<SearchResult>()

        runCatching { out += wikipedia(query, wikiLang(languageTag), 4) }
        runCatching { out += duckduckgo(query, 5) }
        runCatching { out += reddit(query, 4) }

        val braveKey = prefs.searchApiKey
        if (braveKey.isNotBlank()) {
            runCatching { out += brave(query, braveKey, count) }
        }

        return out
            .filter { it.title.isNotBlank() && it.url.isNotBlank() }
            .distinctBy { it.url }
            .take(count)
    }

    private fun wikiLang(tag: String): String = when {
        tag.startsWith("hi") -> "hi"
        tag.startsWith("mr") -> "mr"
        tag.startsWith("bn") -> "bn"
        tag.startsWith("ta") -> "ta"
        tag.startsWith("te") -> "te"
        else -> "en"
    }

    // ---------------------------------------------------------------- Wikipedia
    private suspend fun wikipedia(query: String, lang: String, limit: Int): List<SearchResult> {
        val resp = client.get("https://$lang.wikipedia.org/w/api.php") {
            parameter("action", "query")
            parameter("list", "search")
            parameter("srsearch", query)
            parameter("format", "json")
            parameter("srlimit", limit)
            header(HttpHeaders.Accept, "application/json")
        }
        if (!resp.status.isSuccess()) return emptyList()
        val root = json.parseToJsonElement(resp.bodyAsText()).jsonObject
        val arr = root["query"]?.jsonObject?.get("search")?.jsonArray ?: return emptyList()
        return arr.mapNotNull { el ->
            val o = el.jsonObject
            val title = o["title"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
            val pageId = o["pageid"]?.jsonPrimitive?.contentOrNull
            val snippet = (o["snippet"]?.jsonPrimitive?.contentOrNull ?: "").stripHtml()
            val url = if (pageId != null) "https://$lang.wikipedia.org/?curid=$pageId"
            else "https://$lang.wikipedia.org/wiki/" + title.replace(' ', '_')
            SearchResult(title = "$title — Wikipedia", url = url, snippet = snippet)
        }
    }

    // -------------------------------------------------------------- DuckDuckGo
    private suspend fun duckduckgo(query: String, limit: Int): List<SearchResult> {
        val resp = client.get("https://html.duckduckgo.com/html/") {
            parameter("q", query)
            header(
                HttpHeaders.UserAgent,
                "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0 Mobile Safari/537.36"
            )
            header(HttpHeaders.Accept, "text/html,application/xhtml+xml")
        }
        if (!resp.status.isSuccess()) return emptyList()
        return parseDuckDuckGo(resp.bodyAsText(), limit)
    }

    private fun parseDuckDuckGo(html: String, limit: Int): List<SearchResult> {
        val linkRe = Regex(
            """<a[^>]*class="result__a"[^>]*href="([^"]+)"[^>]*>(.*?)</a>""",
            RegexOption.DOT_MATCHES_ALL
        )
        val snipRe = Regex(
            """class="result__snippet"[^>]*>(.*?)</a>""",
            RegexOption.DOT_MATCHES_ALL
        )
        val links = linkRe.findAll(html).toList()
        val snippets = snipRe.findAll(html).map { it.groupValues[1].stripHtml().trim() }.toList()

        val out = mutableListOf<SearchResult>()
        for ((i, m) in links.withIndex()) {
            if (out.size >= limit) break
            val url = resolveDuckDuckGo(m.groupValues[1])
            val title = m.groupValues[2].stripHtml().trim()
            if (title.isBlank() || url.isBlank()) continue
            out += SearchResult(title, url, snippets.getOrElse(i) { "" })
        }
        return out
    }

    private fun resolveDuckDuckGo(href: String): String {
        val idx = href.indexOf("uddg=")
        if (idx >= 0) {
            val enc = href.substring(idx + 5).substringBefore('&')
            return runCatching { URLDecoder.decode(enc, "UTF-8") }.getOrDefault("")
        }
        return when {
            href.startsWith("http") -> href
            href.startsWith("//") -> "https:$href"
            else -> ""
        }
    }

    // ------------------------------------------------------------------- Reddit
    private suspend fun reddit(query: String, limit: Int): List<SearchResult> {
        val resp = client.get("https://www.reddit.com/search.json") {
            parameter("q", query)
            parameter("limit", limit)
            parameter("sort", "relevance")
            header(HttpHeaders.UserAgent, "android:com.vasudev.ai.master:v1.0.0 (by /u/vasudev-work)")
            header(HttpHeaders.Accept, "application/json")
        }
        if (!resp.status.isSuccess()) return emptyList()
        val root = json.parseToJsonElement(resp.bodyAsText()).jsonObject
        val children = root["data"]?.jsonObject?.get("children")?.jsonArray ?: return emptyList()
        return children.mapNotNull { el ->
            val d = el.jsonObject["data"]?.jsonObject ?: return@mapNotNull null
            val title = d["title"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
            val permalink = d["permalink"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
            val sub = d["subreddit"]?.jsonPrimitive?.contentOrNull ?: ""
            val selftext = d["selftext"]?.jsonPrimitive?.contentOrNull.orEmpty()
            SearchResult(
                title = "$title — r/$sub",
                url = "https://www.reddit.com$permalink",
                snippet = selftext.take(300)
            )
        }
    }

    // -------------------------------------------------------------------- Brave
    private suspend fun brave(query: String, key: String, count: Int): List<SearchResult> {
        val response = client.get(Config.SEARCH_ENDPOINT) {
            parameter("q", query)
            parameter("count", count)
            parameter("country", "in")
            header("X-Subscription-Token", key)
            header(HttpHeaders.Accept, "application/json")
        }
        if (!response.status.isSuccess()) return emptyList()
        val root = json.parseToJsonElement(response.bodyAsText()).jsonObject
        return root["web"]
            ?.jsonObject
            ?.get("results")
            ?.jsonArray
            ?.mapNotNull { element ->
                val obj = element.jsonObject
                val title = obj["title"]?.jsonPrimitive?.contentOrNull
                val url = obj["url"]?.jsonPrimitive?.contentOrNull
                val desc = obj["description"]?.jsonPrimitive?.contentOrNull
                if (title != null && url != null) {
                    SearchResult(title, url, desc.orEmpty().stripHtml())
                } else null
            }
            .orEmpty()
    }
}
