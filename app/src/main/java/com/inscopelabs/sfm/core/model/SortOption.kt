package com.inscopelabs.sfm.core.model

/**
 * Sort options for file listings.
 * Each option includes a primary and secondary sort criteria.
 */
enum class SortOption(
    val displayName: String,
    val isAscending: Boolean = true
) {
    NAME_ASC("Name (A-Z)", true),
    NAME_DESC("Name (Z-A)", false),
    DATE_MODIFIED_ASC("Date Modified (Oldest)", true),
    DATE_MODIFIED_DESC("Date Modified (Newest)", false),
    SIZE_ASC("Size (Smallest)", true),
    SIZE_DESC("Size (Largest)", false),
    TYPE_ASC("Type (A-Z)", true),
    TYPE_DESC("Type (Z-A)", false);

    val primaryComparator: Comparator<FileItem>
        get() = Comparator { a, b -> compare(a, b) }

    fun compare(a: FileItem, b: FileItem): Int {
        // Directories always come first
        if (a.isDirectory != b.isDirectory) {
            return if (a.isDirectory) -1 else 1
        }

        return when (this) {
            NAME_ASC -> a.name.lowercase().compareTo(b.name.lowercase())
            NAME_DESC -> b.name.lowercase().compareTo(a.name.lowercase())
            DATE_MODIFIED_ASC -> a.lastModified.compareTo(b.lastModified)
            DATE_MODIFIED_DESC -> b.lastModified.compareTo(a.lastModified)
            SIZE_ASC -> a.size.compareTo(b.size)
            SIZE_DESC -> b.size.compareTo(a.size)
            TYPE_ASC -> a.extension.lowercase().compareTo(b.extension.lowercase())
            TYPE_DESC -> b.extension.lowercase().compareTo(a.extension.lowercase())
        }
    }

    companion object {
        val default = NAME_ASC
    }
}
