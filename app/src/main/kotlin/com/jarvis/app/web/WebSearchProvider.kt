package com.jarvis.app.web

/**
 * Interface defining search provider operations for online web searches.
 */
interface WebSearchProvider {
    val name: String
    val isConfigured: Boolean
    suspend fun search(query: String, maxResults: Int = 5): List<SearchResult>
}

/**
 * Represents an individual result returned from a web search query.
 */
data class SearchResult(
    val title: String,
    val url: String,
    val snippet: String,
    val source: String = "web"
)
