package net.thunderbird.core.file

import com.eygraber.uri.Uri
import net.thunderbird.core.types.MimeType

/**
 * Resolver for MIME types.
 */
interface MimeTypeResolver {
    fun getMimeType(uri: Uri): MimeType?
}
