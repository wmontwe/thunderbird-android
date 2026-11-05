package com.fsck.k9.backend.imap

import com.fsck.k9.mail.store.imap.ImapStore
import com.fsck.k9.mail.store.imap.OpenMode
import net.thunderbird.components.core.logging.Logger

private const val TAG = "CommandExpunge"

internal class CommandExpunge(
    private val imapStore: ImapStore,
    private val logger: Logger,
) {

    fun expunge(folderServerId: String) {
        logger.debug(tag = TAG) { "processPendingExpunge: folder = $folderServerId" }

        val remoteFolder = imapStore.getFolder(folderServerId)
        try {
            remoteFolder.open(OpenMode.READ_WRITE)

            remoteFolder.expunge()

            logger.debug(tag = TAG) { "processPendingExpunge: complete for folder = $folderServerId" }
        } finally {
            remoteFolder.close()
        }
    }

    fun expungeMessages(folderServerId: String, messageServerIds: List<String>) {
        val remoteFolder = imapStore.getFolder(folderServerId)
        try {
            remoteFolder.open(OpenMode.READ_WRITE)

            remoteFolder.expungeUids(messageServerIds)
        } finally {
            remoteFolder.close()
        }
    }
}
