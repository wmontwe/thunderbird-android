package net.thunderbird.core.file

import com.eygraber.uri.Uri
import com.eygraber.uri.toURI
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import net.thunderbird.core.types.MimeType

class JvmMimeTypeResolver : MimeTypeResolver {

    override fun getMimeType(uri: Uri): MimeType? {
        return getMimeTypeFromContentType(uri) ?: getMimeTypeFromExtension(uri)
    }

    private fun getMimeTypeFromContentType(uri: Uri): MimeType? {
        return try {
            val path = Paths.get(uri.toURI())
            val contentType = Files.probeContentType(path)
            MimeType.fromValue(contentType)
        } catch (_: Exception) {
            null
        }
    }

    private fun getMimeTypeFromExtension(uri: Uri): MimeType? {
        val path = uri.path ?: uri.toString()
        val extension = File(path).extension.lowercase()
        return when (extension) {
            "jpeg", "jpg" -> MimeType.JPEG
            "png" -> MimeType.PNG
            "pdf" -> MimeType.PDF
            else -> null
        }
    }
}
