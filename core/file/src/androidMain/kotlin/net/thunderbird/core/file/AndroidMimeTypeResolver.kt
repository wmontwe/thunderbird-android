package net.thunderbird.core.file

import com.eygraber.uri.Uri
import net.thunderbird.core.types.MimeType

class AndroidMimeTypeResolver(
    private val mimeTypeProvider: AndroidMimeTypeProvider,
) : MimeTypeResolver {

    override fun getMimeType(uri: Uri): MimeType? {
        return MimeType.fromValue(mimeTypeProvider.getType(uri))
    }

    override fun getMimeType(filename: String): MimeType {
        TODO("Not yet implemented")
    }

    override fun getExtension(mimeType: MimeType): Set<String> {
        TODO("Not yet implemented")
    }

    override fun getPreferredExtension(mimeType: MimeType): String? {
        TODO("Not yet implemented")
    }
}
