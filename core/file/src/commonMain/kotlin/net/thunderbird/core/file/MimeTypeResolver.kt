package net.thunderbird.core.file

import com.eygraber.uri.Uri
import net.thunderbird.core.types.MimeType

/**
 * Resolves MIME types for content sources.
 *
 * Implementations of this interface should provide logic to determine the MIME type of a given URI, which may involve
 * checking file extensions, querying content providers, or using other heuristics to identify the correct MIME type.
 */
interface MimeTypeResolver {

    /**
     * Returns the MIME type for the given URI.
     *
     * @param uri The URI for which to determine the MIME type.
     * @return The MIME type associated with the given URI or the default MIME type if the type is unknown.
     */
    fun getMimeType(uri: Uri): MimeType

    /**
     * Returns the MIME type for the given filename.
     *
     * @param filename The filename for which to determine the MIME type.
     * @return The MIME type associated with the given filename or the default MIME type if the extension is unknown.
     */
    fun getMimeType(filename: String): MimeType

    /**
     * Returns a set of file extensions associated with the given MIME type.
     *
     * @param mimeType The MIME type for which to retrieve associated file extensions.
     * @return A set of file extensions that are commonly associated with the given MIME type.
     */
    fun getExtension(mimeType: MimeType): Set<String>

    /**
     * Returns the preferred file extension for the given MIME type.
     *
     * @param mimeType The MIME type for which to retrieve the preferred file extension.
     * @return The preferred file extension associated with the given MIME type, or null if none is defined.
     */
    fun getPreferredExtension(mimeType: MimeType): String?
}
