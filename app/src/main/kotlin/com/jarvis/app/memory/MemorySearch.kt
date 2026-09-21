package com.jarvis.app.memory

import com.jarvis.app.memory.entities.MemoryEntity

/**
 * Provides keyword ranking and contextual memory retrieval using the local Room database.
 */
class MemorySearch(private val dao: MemoryDao) {

    /**
     * Search active memories matching the given query, ranked by relevance and importance.
     */
    suspend fun search(query: String): List<MemoryEntity> {
        val trimmedQuery = query.trim()
        if (trimmedQuery.isEmpty()) return emptyList()

        val rawResults = dao.searchMemories(trimmedQuery)
        val queryTokens = extractTokens(trimmedQuery)

        return rawResults
            .map { memory ->
                val score = calculateRelevanceScore(memory, trimmedQuery, queryTokens)
                Pair(memory, score)
            }
            .sortedWith(
                compareByDescending<Pair<MemoryEntity, Double>> { it.second }
                    .thenByDescending { it.first.importance }
                    .thenByDescending { it.first.updatedAt }
            )
            .map { it.first }
    }

    /**
     * Finds memories related to a given conversational or operational context.
     * Extracts keywords, searches for each keyword, merges, deduplicates, and returns top N by importance.
     */
    suspend fun findRelatedMemories(context: String, limit: Int = 5): List<MemoryEntity> {
        if (context.isBlank() || limit <= 0) return emptyList()

        val keywords = extractKeywords(context)
        if (keywords.isEmpty()) return emptyList()

        val memoryMap = mutableMapOf<Long, MemoryEntity>()
        for (keyword in keywords) {
            val results = dao.searchMemories(keyword)
            for (memory in results) {
                memoryMap.putIfAbsent(memory.id, memory)
            }
        }

        return memoryMap.values
            .sortedWith(
                compareByDescending<MemoryEntity> { it.importance }
                    .thenByDescending { it.updatedAt }
            )
            .take(limit)
    }

    private fun calculateRelevanceScore(
        memory: MemoryEntity,
        fullQuery: String,
        queryTokens: List<String>
    ): Double {
        var score = 0.0
        val contentLower = memory.content.lowercase()
        val tagsLower = memory.tags.lowercase()
        val fullLower = fullQuery.lowercase()

        // Exact phrase matches
        if (contentLower.contains(fullLower)) {
            score += 10.0
        }
        if (tagsLower.contains(fullLower)) {
            score += 15.0
        }

        // Token matches
        for (token in queryTokens) {
            if (token.isEmpty()) continue
            val tokenLower = token.lowercase()
            if (contentLower.contains(tokenLower)) {
                score += 2.0
            }
            if (tagsLower.split(",").map { it.trim() }.any { it.equals(tokenLower, ignoreCase = true) }) {
                score += 5.0
            } else if (tagsLower.contains(tokenLower)) {
                score += 3.0
            }
        }

        // Weight by memory importance
        score += memory.importance * 0.5

        return score
    }

    private fun extractTokens(text: String): List<String> {
        return text.split(Regex("[\\s,;:.!?\"'()\\[\\]{}]+"))
            .filter { it.isNotBlank() }
    }

    private fun extractKeywords(context: String): List<String> {
        val stopWords = setOf(
            "a", "about", "above", "after", "again", "against", "all", "am", "an", "and",
            "any", "are", "aren't", "as", "at", "be", "because", "been", "before", "being",
            "below", "between", "both", "but", "by", "can't", "cannot", "could", "couldn't",
            "did", "didn't", "do", "does", "doesn't", "doing", "don't", "down", "during",
            "each", "few", "for", "from", "further", "had", "hadn't", "has", "hasn't",
            "have", "haven't", "having", "he", "he'd", "he'll", "he's", "her", "here",
            "here's", "hers", "herself", "him", "himself", "his", "how", "how's", "i",
            "i'd", "i'll", "i'm", "i've", "if", "in", "into", "is", "isn't", "it", "it's",
            "its", "itself", "let's", "me", "more", "most", "mustn't", "my", "myself",
            "no", "nor", "not", "of", "off", "on", "once", "only", "or", "other", "ought",
            "our", "ours", "ourselves", "out", "over", "own", "same", "shan't", "she",
            "she'd", "she'll", "she's", "should", "shouldn't", "so", "some", "such",
            "than", "that", "that's", "the", "their", "theirs", "them", "themselves",
            "then", "there", "there's", "these", "they", "they'd", "they'll", "they're",
            "they've", "this", "those", "through", "to", "too", "under", "until", "up",
            "very", "was", "wasn't", "we", "we'd", "we'll", "we're", "we've", "were",
            "weren't", "what", "what's", "when", "when's", "where", "where's", "which",
            "while", "who", "who's", "whom", "why", "why's", "with", "won't", "would",
            "wouldn't", "you", "you'd", "you'll", "you're", "you've", "your", "yours",
            "yourself", "yourselves", "jarvis", "please"
        )

        val words = extractTokens(context)
            .map { it.lowercase() }
            .filter { it.length > 2 && it !in stopWords }

        // Frequency-ranked distinct keywords, up to 10
        return words.groupingBy { it }
            .eachCount()
            .entries
            .sortedByDescending { it.value }
            .map { it.key }
            .take(10)
    }
}
