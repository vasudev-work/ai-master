package com.vasudev.ai.master.core

/**
 * Small shared helpers used across the app.
 * (In the reference layout this is the "utils.py / helpers.py" file.)
 */

/** Removes HTML tags from a snippet returned by the web-search provider. */
fun String.stripHtml(): String = replace(Regex("<[^>]*>"), "")

/** The bare host of a result URL, e.g. "en.wikipedia.org". */
fun SearchResult.domain(): String = url.substringAfter("//").substringBefore('/')

/**
 * Turns the research planner's output into a short list of search queries.
 * Falls back to the user's own question if nothing usable came back.
 */
fun parseSearchQueries(plan: String, fallback: String): List<String> {
    val cleaned = plan.lines()
        .map { it.trim().removePrefix("-").removePrefix("*").trim() }
        .map { it.replace(Regex("^\\d+[.)]\\s*"), "").trim() }
        .map { it.trim('"', '\'', ' ') }
        .filter { it.isNotBlank() && it.length in 3..140 }
        .distinct()
        .take(3)
    return cleaned.ifEmpty { listOf(fallback) }
}
