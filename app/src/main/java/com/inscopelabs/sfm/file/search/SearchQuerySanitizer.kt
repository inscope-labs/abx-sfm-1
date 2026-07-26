package com.inscopelabs.sfm.file.search

import com.inscopelabs.sfm.core.exception.InvalidFileOperationException

/**
 * Sanitizes and validates search queries.
 */
object SearchQuerySanitizer {

    /**
     * Sanitizes a search query string.
     */
    fun sanitizeQuery(query: String): String {
        if (query.isBlank()) return ""

        var sanitized = query

        // Remove potentially dangerous characters
        val dangerousChars = charArrayOf(
            '\\', '[', ']', '{', '}', '(', ')', '|', '^', '$',
            '*', '+', '?', '<', '>', '\u0000'
        )

        dangerousChars.forEach { char ->
            sanitized = sanitized.replace(char.toString(), "")
        }

        // Normalize whitespace
        sanitized = sanitized.trim().replace(Regex("\\s+"), " ")

        // Limit length
        if (sanitized.length > MAX_QUERY_LENGTH) {
            sanitized = sanitized.take(MAX_QUERY_LENGTH)
        }

        return sanitized
    }

    /**
     * Validates a search filter.
     */
    fun validateFilter(filter: SearchFilter): SearchFilter {
        return when (filter) {
            is SearchFilter.TextFilter -> {
                filter.copy(
                    query = sanitizeQuery(filter.query)
                )
            }
            is SearchFilter.ExtensionFilter -> {
                filter.copy(
                    extensions = filter.extensions
                        .map { it.lowercase().removePrefix(".") }
                        .filter { isValidExtension(it) }
                        .take(MAX_EXTENSIONS)
                )
            }
            is SearchFilter.SizeFilter -> {
                if (filter.minSize < 0 || filter.maxSize < 0) {
                    throw InvalidFileOperationException("Size values cannot be negative")
                }
                if (filter.minSize > filter.maxSize) {
                    throw InvalidFileOperationException("Minimum size cannot exceed maximum size")
                }
                filter
            }
            is SearchFilter.DateFilter -> {
                if (filter.from > filter.to) {
                    throw InvalidFileOperationException("Start date cannot be after end date")
                }
                filter
            }
            is SearchFilter.TypeFilter -> filter
            is SearchFilter.CompositeFilter -> filter
        }
    }

    /**
     * Validates a file extension.
     */
    private fun isValidExtension(extension: String): Boolean {
        if (extension.isBlank() || extension.length > 10) {
            return false
        }
        // Only allow alphanumeric characters
        return extension.all { it.isLetterOrDigit() }
    }

    /**
     * Builds a safe SQL LIKE pattern from query.
     */
    fun buildLikePattern(query: String): String {
        val sanitized = sanitizeQuery(query)
        return "%$sanitized%"
    }

    /**
     * Validates search options.
     */
    fun validateOptions(options: SearchOptions): SearchOptions {
        return options.copy(
            query = sanitizeQuery(options.query),
            extensions = options.extensions
                .map { it.lowercase().removePrefix(".") }
                .filter { isValidExtension(it) }
                .take(MAX_EXTENSIONS)
        )
    }

    data class SearchOptions(
        val query: String = "",
        val extensions: List<String> = emptyList(),
        val maxResults: Int = 100,
        val includeHidden: Boolean = false
    )

    sealed class SearchFilter {
        data class TextFilter(val query: String, val caseSensitive: Boolean = false) : SearchFilter()
        data class ExtensionFilter(val extensions: List<String>) : SearchFilter()
        data class SizeFilter(val minSize: Long, val maxSize: Long) : SearchFilter()
        data class DateFilter(val from: Long, val to: Long) : SearchFilter()
        data class TypeFilter(val types: List<String>) : SearchFilter()
        data class CompositeFilter(
            val filters: List<SearchFilter>,
            val combineMode: CombineMode = CombineMode.AND
        ) : SearchFilter()
    }

    enum class CombineMode {
        AND, OR
    }

    private const val MAX_QUERY_LENGTH = 100
    private const val MAX_EXTENSIONS = 20
}
