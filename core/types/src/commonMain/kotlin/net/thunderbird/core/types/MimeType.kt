package net.thunderbird.core.types

/**
 * MIME type representation.
 */
enum class MimeType(val values: List<String>) {
    JPEG(listOf("image/jpeg", "image/jpg")),
    PNG(listOf("image/png")),
    PDF(listOf("application/pdf")),

    /**
     * Fallback for unknown MIME types. The value is the original MIME type string.
     */
    UNKNOWN(emptyList()),
    ;

    val value: String = values.first()

    companion object {
        private val lookup: Map<String, MimeType> = entries.flatMap { entry ->
            entry.values.map { value -> value.lowercase() to entry }
        }.toMap()

        fun fromValue(value: String?): MimeType? {
            if (value == null) return null
            return lookup[value.lowercase()]
        }
    }
}
