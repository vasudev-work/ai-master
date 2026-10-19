package com.vasudev.ai.master.core

/**
 * Small shared helpers used across the app.
 * (In the reference layout this is the "utils.py / helpers.py" file.)
 */

/** Removes HTML tags from a snippet returned by the web-search provider. */
fun String.stripHtml(): String = replace(Regex("<[^>]*>"), "")

/** The bare host of a result URL, e.g. "en.wikipedia.org". */
fun SearchResult.domain(): String = url.substringAfter("//").substringBefore('/')
