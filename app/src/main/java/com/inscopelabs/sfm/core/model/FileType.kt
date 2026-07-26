package com.inscopelabs.sfm.core.model

/**
 * Enumeration of file types with associated MIME type categories.
 */
enum class FileType(
    val displayName: String,
    val mimeCategory: String,
    val isPreviewable: Boolean = true,
    val isExecutable: Boolean = false
) {
    // Directories
    DIRECTORY("Directory", "directory", isPreviewable = false),

    // Documents
    PDF("PDF Document", "application/pdf"),
    DOC("Word Document", "application/msword"),
    DOCX("Word Document", "application/vnd.openxmlformats-officedocument.wordprocessingml.document"),
    XLS("Excel Spreadsheet", "application/vnd.ms-excel"),
    XLSX("Excel Spreadsheet", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"),
    PPT("PowerPoint", "application/vnd.ms-powerpoint"),
    PPTX("PowerPoint", "application/vnd.openxmlformats-officedocument.presentationml.presentation"),
    TXT("Text File", "text/plain"),
    RTF("Rich Text", "text/rtf"),

    // Images
    IMAGE("Image", "image/*", isPreviewable = true),
    JPEG("JPEG Image", "image/jpeg"),
    PNG("PNG Image", "image/png"),
    GIF("GIF Image", "image/gif"),
    WEBP("WebP Image", "image/webp"),
    SVG("SVG Image", "image/svg+xml"),

    // Audio
    AUDIO("Audio", "audio/*"),
    MP3("MP3 Audio", "audio/mpeg"),
    WAV("WAV Audio", "audio/wav"),
    FLAC("FLAC Audio", "audio/flac"),
    OGG("OGG Audio", "audio/ogg"),

    // Video
    VIDEO("Video", "video/*"),
    MP4("MP4 Video", "video/mp4"),
    MKV("MKV Video", "video/x-matroska"),
    AVI("AVI Video", "video/x-msvideo"),
    WEBM("WebM Video", "video/webm"),

    // Archives
    ARCHIVE("Archive", "application/zip", isExecutable = false),
    ZIP("ZIP Archive", "application/zip"),
    TAR("TAR Archive", "application/x-tar"),
    GZ("GZIP Archive", "application/gzip"),
    RAR("RAR Archive", "application/vnd.rar"),

    // Code
    CODE("Code", "text/*"),
    JSON("JSON", "application/json"),
    XML("XML", "application/xml"),
    HTML("HTML", "text/html"),
    CSS("CSS", "text/css"),
    JAVASCRIPT("JavaScript", "application/javascript"),
    KOTLIN("Kotlin", "text/x-kotlin"),
    JAVA("Java", "text/x-java"),
    PYTHON("Python", "text/x-python"),

    // Executables
    APK("Android APK", "application/vnd.android.package-archive", isExecutable = true),
    SHELL("Shell Script", "application/x-sh", isExecutable = true),

    // Unknown
    UNKNOWN("Unknown", "*/*", isPreviewable = false, isExecutable = false);

    companion object {
        fun fromMimeType(mimeType: String?, isDirectory: Boolean): FileType {
            if (isDirectory) return DIRECTORY
            if (mimeType == null) return UNKNOWN

            return when {
                mimeType.startsWith("image/") -> when (mimeType) {
                    "image/jpeg" -> JPEG
                    "image/png" -> PNG
                    "image/gif" -> GIF
                    "image/webp" -> WEBP
                    "image/svg+xml" -> SVG
                    else -> IMAGE
                }
                mimeType.startsWith("audio/") -> when (mimeType) {
                    "audio/mpeg" -> MP3
                    "audio/wav" -> WAV
                    "audio/flac" -> FLAC
                    "audio/ogg" -> OGG
                    else -> AUDIO
                }
                mimeType.startsWith("video/") -> when (mimeType) {
                    "video/mp4" -> MP4
                    "video/x-matroska" -> MKV
                    "video/x-msvideo" -> AVI
                    "video/webm" -> WEBM
                    else -> VIDEO
                }
                mimeType == "application/pdf" -> PDF
                mimeType.contains("word") -> DOCX
                mimeType.contains("excel") || mimeType.contains("spreadsheet") -> XLSX
                mimeType.contains("powerpoint") || mimeType.contains("presentation") -> PPTX
                mimeType == "text/plain" -> TXT
                mimeType == "text/rtf" -> RTF
                mimeType == "application/json" -> JSON
                mimeType == "application/xml" || mimeType == "text/xml" -> XML
                mimeType == "text/html" -> HTML
                mimeType == "text/css" -> CSS
                mimeType == "application/javascript" -> JAVASCRIPT
                mimeType.contains("kotlin") -> KOTLIN
                mimeType.contains("java") -> JAVA
                mimeType.contains("python") -> PYTHON
                mimeType == "application/zip" -> ZIP
                mimeType == "application/x-tar" -> TAR
                mimeType == "application/gzip" -> GZ
                mimeType == "application/vnd.rar" -> RAR
                mimeType == "application/vnd.android.package-archive" -> APK
                mimeType == "application/x-sh" -> SHELL
                else -> UNKNOWN
            }
        }
    }
}
