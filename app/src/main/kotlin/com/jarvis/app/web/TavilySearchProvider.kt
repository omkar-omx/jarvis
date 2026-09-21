package com.jarvis.app.web

import android.util.Log
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class TavilyRequest(
    val api_key: String,
    val query: String,
    val search_depth: String = "basic",
    val max_results: Int = 5,
    val include_answer: Boolean = true
)

data class TavilyResponse(
    val answer: String?,
    val query: String,
    val results: List<TavilyResult> = emptyList()
)

data class TavilyResult(
    val title: String = "",
    val url: String = "",
    val content: String = ""
)

interface TavilyApi {
    @POST("search")
    suspend fun search(@Body request: TavilyRequest): TavilyResponse
}

class TavilySearchProvider(private val apiKey: String) : WebSearchProvider {

    override val name: String = "Tavily"
    override val isConfigured: Boolean = apiKey.isNotBlank()

    private val retrofit = Retrofit.Builder()
        .baseUrl("https://api.tavily.com/")
        .addConverterFactory(MoshiConverterFactory.create())
        .build()

    private val api = retrofit.create(TavilyApi::class.java)

    override suspend fun search(query: String, maxResults: Int): List<SearchResult> {
        if (!isConfigured) return emptyList()

        return withContext(Dispatchers.IO) {
            try {
                val response = api.search(
                    TavilyRequest(
                        api_key = apiKey,
                        query = query,
                        max_results = maxResults
                    )
                )
                val list = mutableListOf<SearchResult>()
                response.answer?.let { ans ->
                    if (ans.isNotBlank()) {
                        list.add(SearchResult(title = "Summary Answer", url = "https://tavily.com", snippet = ans, source = "Tavily AI"))
                    }
                }
                response.results.forEach { res ->
                    list.add(SearchResult(title = res.title, url = res.url, snippet = res.content, source = "Tavily Search"))
                }
                list
            } catch (e: Exception) {
                Log.e("TavilySearch", "Search failed", e)
                emptyList()
            }
        }
    }
}
