package com.fsck.k9.backend.pop3

import com.fsck.k9.backend.api.BackendFolder
import com.fsck.k9.backend.api.BackendStorage
import com.fsck.k9.backend.api.SyncConfig
import com.fsck.k9.backend.api.SyncListener
import com.fsck.k9.mail.AuthenticationFailedException
import com.fsck.k9.mail.FetchProfile
import com.fsck.k9.mail.MessageDownloadState
import com.fsck.k9.mail.MessageRetrievalListener
import com.fsck.k9.mail.store.pop3.Pop3Folder
import com.fsck.k9.mail.store.pop3.Pop3Message
import com.fsck.k9.mail.store.pop3.Pop3Store
import java.lang.Exception
import java.util.ArrayList
import java.util.Date
import java.util.HashMap
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.runBlocking
import net.thunderbird.components.core.logging.Logger
import net.thunderbird.core.common.exception.MessagingException
import net.thunderbird.core.common.exception.rootCauseMessage
import net.thunderbird.core.common.mail.Flag

private const val TAG = "Pop3Sync"

@Suppress("TooManyFunctions")
internal class Pop3Sync(
    private val accountName: String,
    private val backendStorage: BackendStorage,
    private val remoteStore: Pop3Store,
    private val logger: Logger,
) {

    fun sync(folder: String, syncConfig: SyncConfig, listener: SyncListener) {
        synchronizeMailboxSynchronous(folder, syncConfig, listener)
    }

    @Suppress(
        "TooGenericExceptionCaught",
        "TooGenericExceptionThrown",
        "LongMethod",
        "CyclomaticComplexMethod",
        "NestedBlockDepth",
    )
    fun synchronizeMailboxSynchronous(folder: String, syncConfig: SyncConfig, listener: SyncListener) {
        var remoteFolder: Pop3Folder? = null

        logger.info(tag = TAG) { "Synchronizing folder $accountName:$folder" }

        var backendFolder: BackendFolder? = null
        try {
            logger.debug(tag = TAG) { "SYNC: About to process pending commands for account $accountName" }

            logger.verbose(tag = TAG) { "SYNC: About to get local folder $folder" }
            backendFolder = backendStorage.getFolder(folder)

            listener.syncStarted(folder)

            /*
             * Get the message list from the local store and create an index of
             * the uids within the list.
             */
            var localUidMap: Map<String, Long?> = backendFolder.getAllMessagesAndEffectiveDates()

            logger.verbose(tag = TAG) { "SYNC: About to get remote folder $folder" }
            remoteFolder = remoteStore.getFolder(folder)

            /*
             * Synchronization process:
             *
            Open the folder
            Upload any local messages that are marked as PENDING_UPLOAD (Drafts, Sent, Trash)
            Get the message count
            Get the list of the newest K9.DEFAULT_VISIBLE_LIMIT messages
            getMessages(messageCount - K9.DEFAULT_VISIBLE_LIMIT, messageCount)
            See if we have each message locally, if not fetch it's flags and envelope
            Get and update the unread count for the folder
            Update the remote flags of any messages we have locally with an internal date newer than the remote message.
            Get the current flags for any messages we have locally but did not just download
            Update local flags
            For any message we have locally but not remotely, delete the local message to keep cache clean.
            Download larger parts of any new messages.
            (Optional) Download small attachments in the background.
             */

            /*
             * Open the remote folder. This pre-loads certain metadata like message count.
             */
            logger.verbose(tag = TAG) { "SYNC: About to open remote folder $folder" }

            remoteFolder.open()

            listener.syncAuthenticationSuccess()

            /*
             * Get the remote message count.
             */
            val remoteMessageCount = remoteFolder.messageCount

            var visibleLimit = backendFolder.visibleLimit

            if (visibleLimit < 0) {
                visibleLimit = syncConfig.defaultVisibleLimit
            }

            val remoteMessages: MutableList<Pop3Message?> = ArrayList<Pop3Message?>()
            val remoteUidMap: MutableMap<String?, Pop3Message?> = HashMap<String?, Pop3Message?>()

            logger.verbose(tag = TAG) { "SYNC: Remote message count for folder $folder is $remoteMessageCount" }

            val earliestDate = syncConfig.earliestPollDate
            val earliestTimestamp = if (earliestDate != null) earliestDate.time else 0L

            /* Message numbers start at 1. */
            var remoteStart = 1
            if (remoteMessageCount > 0) {
                // Adjust the starting message number based on the visible limit
                if (visibleLimit > 0) {
                    remoteStart += (remoteMessageCount - visibleLimit).coerceAtLeast(0)
                }

                logger.verbose(tag = TAG) {
                    "SYNC: About to get messages $remoteStart through $remoteMessageCount for folder $folder"
                }

                val headerProgress = AtomicInteger(0)
                listener.syncHeadersStarted(folder)

                val remoteMessageArray = remoteFolder.getMessages(remoteStart, remoteMessageCount, null)

                val messageCount = remoteMessageArray.size

                for (thisMess in remoteMessageArray) {
                    headerProgress.incrementAndGet()
                    listener.syncHeadersProgress(
                        folderServerId = folder,
                        completed = headerProgress.get(),
                        total = messageCount,
                    )

                    val localMessageTimestamp = localUidMap[thisMess.uid]
                    if (localMessageTimestamp == null || localMessageTimestamp >= earliestTimestamp) {
                        remoteMessages.add(thisMess)
                        remoteUidMap[thisMess.uid] = thisMess
                    }
                }

                logger.verbose(tag = TAG) { "SYNC: Got ${remoteUidMap.size} messages for folder $folder" }

                listener.syncHeadersFinished(
                    folderServerId = folder,
                    totalMessagesInMailbox = headerProgress.get(),
                    numNewMessages = remoteUidMap.size,
                )
            } else if (remoteMessageCount < 0) {
                throw Exception("Message count $remoteMessageCount for folder $folder")
            }

            /*
             * Remove any messages that are in the local store but no longer on the remote store or are too old
             */
            var moreMessages = backendFolder.getMoreMessages()
            if (syncConfig.syncRemoteDeletions) {
                val destroyMessageUids: MutableList<String> = ArrayList<String>()
                for (localMessageUid in localUidMap.keys) {
                    if (remoteUidMap[localMessageUid] == null) {
                        destroyMessageUids.add(localMessageUid)
                    }
                }

                if (!destroyMessageUids.isEmpty()) {
                    moreMessages = BackendFolder.MoreMessages.UNKNOWN

                    backendFolder.destroyMessages(destroyMessageUids)
                    for (uid in destroyMessageUids) {
                        listener.syncRemovedMessage(folderServerId = folder, messageServerId = uid)
                    }
                }
            }

            if (moreMessages == BackendFolder.MoreMessages.UNKNOWN) {
                updateMoreMessages(remoteFolder, backendFolder, remoteStart)
            }

            /*
             * Now we download the actual content of messages.
             */
            val newMessages = downloadMessages(
                syncConfig = syncConfig,
                remoteFolder = remoteFolder,
                backendFolder = backendFolder,
                inputMessages = remoteMessages,
                listener = listener,
            )

            listener.folderStatusChanged(folder)

            /* Notify listeners that we're finally done. */
            backendFolder.setLastChecked(System.currentTimeMillis())
            backendFolder.setStatus(null)

            logger.debug(tag = TAG) {
                "Done synchronizing folder $accountName:$folder @ ${Date()} with $newMessages new messages"
            }

            listener.syncFinished(folder)

            logger.info(tag = TAG) { "Done synchronizing folder $accountName:$folder" }
        } catch (e: AuthenticationFailedException) {
            listener.syncFailed(folderServerId = folder, message = "Authentication failure", exception = e)
        } catch (e: Exception) {
            logger.error(tag = TAG, throwable = e) { "synchronizeMailbox" }
            // If we don't set the last checked, it can try too often during
            // failure conditions
            val rootMessage = e.rootCauseMessage.orEmpty()
            if (backendFolder != null) {
                try {
                    backendFolder.setStatus(rootMessage)
                    backendFolder.setLastChecked(System.currentTimeMillis())
                } catch (e1: Exception) {
                    logger.error(tag = TAG, throwable = e1) {
                        "Could not set last checked on folder $accountName:$folder"
                    }
                }
            }

            listener.syncFailed(folderServerId = folder, message = rootMessage, exception = e)

            logger.error(tag = TAG) {
                "Failed synchronizing folder $accountName:$folder @ ${Date()}"
            }
        } finally {
            remoteFolder?.close()
        }
    }

    private fun updateMoreMessages(
        remoteFolder: Pop3Folder,
        backendFolder: BackendFolder,
        remoteStart: Int,
    ) {
        if (remoteStart == 1) {
            backendFolder.setMoreMessages(BackendFolder.MoreMessages.FALSE)
        } else {
            val moreMessagesAvailable = remoteFolder.areMoreMessagesAvailable(remoteStart)

            val newMoreMessages =
                if ((moreMessagesAvailable)) BackendFolder.MoreMessages.TRUE else BackendFolder.MoreMessages.FALSE
            backendFolder.setMoreMessages(newMoreMessages)
        }
    }

    @Suppress("TooGenericExceptionCaught", "LongMethod")
    @Throws(MessagingException::class)
    private fun downloadMessages(
        syncConfig: SyncConfig,
        remoteFolder: Pop3Folder,
        backendFolder: BackendFolder,
        inputMessages: MutableList<Pop3Message?>,
        listener: SyncListener,
    ): Int {
        val earliestDate = syncConfig.earliestPollDate
        val downloadStarted = Date() // now

        if (earliestDate != null) {
            logger.debug(tag = TAG) { "Only syncing messages after $earliestDate" }
        }
        val folder = remoteFolder.serverId

        val syncFlagMessages: MutableList<Pop3Message?> = ArrayList<Pop3Message?>()
        var unsyncedMessages: MutableList<Pop3Message> = ArrayList<Pop3Message>()
        val newMessages = AtomicInteger(0)

        val messages: MutableList<Pop3Message> = ArrayList<Pop3Message>(inputMessages)

        for (message in messages) {
            evaluateMessageForDownload(message, folder, backendFolder, unsyncedMessages, syncFlagMessages, listener)
        }

        val progress = AtomicInteger(0)
        val todo = unsyncedMessages.size + syncFlagMessages.size
        listener.syncProgress(folderServerId = folder, completed = progress.get(), total = todo)

        logger.debug(tag = TAG) { "SYNC: Have ${unsyncedMessages.size} unsynced messages" }

        messages.clear()
        val largeMessages: MutableList<Pop3Message> = ArrayList<Pop3Message>()
        val smallMessages: MutableList<Pop3Message> = ArrayList<Pop3Message>()
        if (!unsyncedMessages.isEmpty()) {
            val visibleLimit = backendFolder.visibleLimit
            val listSize = unsyncedMessages.size

            if ((visibleLimit > 0) && (listSize > visibleLimit)) {
                unsyncedMessages = unsyncedMessages.subList(fromIndex = 0, toIndex = visibleLimit)
            }

            val fp = FetchProfile()
            fp.add(FetchProfile.Item.ENVELOPE)

            logger.debug(tag = TAG) {
                "SYNC: About to fetch ${unsyncedMessages.size} unsynced messages for folder $folder"
            }

            fetchUnsyncedMessages(
                syncConfig = syncConfig,
                remoteFolder = remoteFolder,
                unsyncedMessages = unsyncedMessages,
                smallMessages = smallMessages,
                largeMessages = largeMessages,
                progress = progress,
                todo = todo,
                fp = fp,
                listener = listener,
            )

            logger.debug(tag = TAG) { "SYNC: Synced unsynced messages for folder $folder" }
        }

        logger.debug(tag = TAG) {
            "SYNC: Have ${largeMessages.size} large messages and ${smallMessages.size} small messages " +
                "out of ${unsyncedMessages.size} unsynced messages"
        }

        unsyncedMessages.clear()
        /*
         * Grab the content of the small messages first. This is going to
         * be very fast and at very worst will be a single up of a few bytes and a single
         * download of 625k.
         */
        var fp = FetchProfile()
        // TODO: Only fetch small and large messages if we have some
        fp.add(FetchProfile.Item.BODY)
        //        fp.add(FetchProfile.Item.FLAGS);
        //        fp.add(FetchProfile.Item.ENVELOPE);
        downloadSmallMessages(
            remoteFolder = remoteFolder,
            backendFolder = backendFolder,
            smallMessages = smallMessages,
            progress = progress,
            newMessages = newMessages,
            todo = todo,
            fp = fp,
            listener = listener,
        )
        smallMessages.clear()
        /*
         * Now do the large messages that require more round trips.
         */
        fp = FetchProfile()
        fp.add(FetchProfile.Item.STRUCTURE)
        downloadLargeMessages(
            syncConfig = syncConfig,
            remoteFolder = remoteFolder,
            backendFolder = backendFolder,
            largeMessages = largeMessages,
            progress = progress,
            newMessages = newMessages,
            todo = todo,
            fp = fp,
            listener = listener,
        )
        largeMessages.clear()

        logger.debug(tag = TAG) { "SYNC: Synced remote messages for folder $folder, ${newMessages.get()} new messages" }

        // If the oldest message seen on this sync is newer than the oldest message seen on the previous sync, then
        // we want to move our high-water mark forward.
        val oldestMessageTime = backendFolder.getOldestMessageDate()
        if (oldestMessageTime != null) {
            if (oldestMessageTime.before(downloadStarted) &&
                oldestMessageTime.after(getLatestOldMessageSeenTime(backendFolder))
            ) {
                setLatestOldMessageSeenTime(backendFolder, oldestMessageTime)
            }
        }

        return newMessages.get()
    }

    private fun getLatestOldMessageSeenTime(backendFolder: BackendFolder): Date {
        val latestOldMessageSeenTime = backendFolder.getFolderExtraNumber(EXTRA_LATEST_OLD_MESSAGE_SEEN_TIME)
        val timestamp = if (latestOldMessageSeenTime != null) latestOldMessageSeenTime else 0L
        return Date(timestamp)
    }

    private fun setLatestOldMessageSeenTime(backendFolder: BackendFolder, oldestMessageTime: Date) {
        backendFolder.setFolderExtraNumber(EXTRA_LATEST_OLD_MESSAGE_SEEN_TIME, oldestMessageTime.time)
    }

    private fun evaluateMessageForDownload(
        message: Pop3Message,
        folder: String,
        backendFolder: BackendFolder,
        unsyncedMessages: MutableList<Pop3Message>,
        syncFlagMessages: MutableList<Pop3Message?>,
        listener: SyncListener,
    ) {
        val messageServerId = message.uid
        if (message.isSet(Flag.DELETED)) {
            logger.verbose(tag = TAG) { "Message with uid $messageServerId is marked as deleted" }

            syncFlagMessages.add(message)
            return
        }

        val messagePresentLocally = backendFolder.isMessagePresent(messageServerId)

        if (!messagePresentLocally) {
            if (!message.isSet(Flag.X_DOWNLOADED_FULL) && !message.isSet(Flag.X_DOWNLOADED_PARTIAL)) {
                logger.verbose(tag = TAG) { "Message with uid $messageServerId has not yet been downloaded" }

                unsyncedMessages.add(message)
            } else {
                logger.verbose(tag = TAG) { "Message with uid $messageServerId is partially or fully downloaded" }

                // Store the updated message locally
                val completeMessage = message.isSet(Flag.X_DOWNLOADED_FULL)
                runBlocking {
                    if (completeMessage) {
                        backendFolder.saveMessage(message, MessageDownloadState.FULL)
                    } else {
                        backendFolder.saveMessage(message, MessageDownloadState.PARTIAL)
                    }
                }
                val isOldMessage = isOldMessage(backendFolder, message)
                listener.syncNewMessage(
                    folderServerId = folder,
                    messageServerId = messageServerId,
                    isOldMessage = isOldMessage,
                )
            }
            return
        }

        val messageFlags: Set<Flag> = backendFolder.getMessageFlags(messageServerId)
        if (!messageFlags.contains(Flag.DELETED)) {
            logger.verbose(tag = TAG) { "Message with uid $messageServerId is present in the local store" }

            if (!messageFlags.contains(Flag.X_DOWNLOADED_FULL) && !messageFlags.contains(Flag.X_DOWNLOADED_PARTIAL)) {
                logger.verbose(tag = TAG) {
                    "Message with uid $messageServerId is not downloaded, even partially; trying again"
                }

                unsyncedMessages.add(message)
            } else {
                syncFlagMessages.add(message)
            }
        } else {
            logger.verbose(tag = TAG) { "Local copy of message with uid $messageServerId is marked as deleted" }
        }
    }

    @Suppress("LongParameterList")
    @Throws(MessagingException::class)
    private fun fetchUnsyncedMessages(
        syncConfig: SyncConfig,
        remoteFolder: Pop3Folder,
        unsyncedMessages: MutableList<Pop3Message>?,
        smallMessages: MutableList<Pop3Message>,
        largeMessages: MutableList<Pop3Message>,
        progress: AtomicInteger,
        todo: Int,
        fp: FetchProfile?,
        listener: SyncListener,
    ) {
        val folder = remoteFolder.serverId

        val earliestDate = syncConfig.earliestPollDate
        @Suppress("TooGenericExceptionCaught")
        remoteFolder.fetch(
            unsyncedMessages,
            fp,
            MessageRetrievalListener { message ->
                try {
                    if (message.isSet(Flag.DELETED) || message.olderThan(earliestDate)) {
                        if (message.isSet(Flag.DELETED)) {
                            logger.verbose(tag = TAG) {
                                "Newly downloaded message $accountName:$folder:${message.uid} " +
                                    "was marked deleted on server, skipping"
                            }
                        } else {
                            logger.debug(tag = TAG) {
                                "Newly downloaded message ${message.uid} is older than $earliestDate, skipping"
                            }
                        }

                        progress.incrementAndGet()

                        // TODO: This might be the source of poll count errors in the UI.
                        // Is todo always the same as ofTotal
                        listener.syncProgress(folderServerId = folder, completed = progress.get(), total = todo)
                        return@MessageRetrievalListener
                    }

                    if (syncConfig.maximumAutoDownloadMessageSize > 0 &&
                        message.size > syncConfig.maximumAutoDownloadMessageSize
                    ) {
                        largeMessages.add(message)
                    } else {
                        smallMessages.add(message)
                    }
                } catch (e: Exception) {
                    logger.error(tag = TAG, throwable = e) { "Error while storing downloaded message." }
                }
            },
            syncConfig.maximumAutoDownloadMessageSize,
        )
    }

    @Suppress("LongParameterList")
    @Throws(MessagingException::class)
    private fun downloadSmallMessages(
        remoteFolder: Pop3Folder,
        backendFolder: BackendFolder,
        smallMessages: MutableList<Pop3Message>,
        progress: AtomicInteger,
        newMessages: AtomicInteger,
        todo: Int,
        fp: FetchProfile?,
        listener: SyncListener,
    ) {
        val folder = remoteFolder.serverId

        logger.debug(tag = TAG) { "SYNC: Fetching ${smallMessages.size} small messages for folder $folder" }

        @Suppress("TooGenericExceptionCaught")
        remoteFolder.fetch(
            smallMessages,
            fp,
            MessageRetrievalListener { message ->
                try {
                    // Store the updated message locally

                    runBlocking {
                        backendFolder.saveMessage(message, MessageDownloadState.FULL)
                    }
                    progress.incrementAndGet()

                    // Increment the number of "new messages" if the newly downloaded message is
                    // not marked as read.
                    if (!message.isSet(Flag.SEEN)) {
                        newMessages.incrementAndGet()
                    }

                    val messageServerId = message.uid
                    logger.verbose(tag = TAG) {
                        "About to notify listeners that we got a new small message " +
                            "$accountName:$folder:$messageServerId"
                    }

                    // Update the listener with what we've found
                    listener.syncProgress(folderServerId = folder, completed = progress.get(), total = todo)

                    val isOldMessage = isOldMessage(backendFolder, message)
                    listener.syncNewMessage(
                        folderServerId = folder,
                        messageServerId = messageServerId,
                        isOldMessage = isOldMessage,
                    )
                } catch (e: Exception) {
                    logger.error(tag = TAG, throwable = e) { "SYNC: fetch small messages" }
                }
            },
            -1,
        )

        logger.debug(tag = TAG) { "SYNC: Done fetching small messages for folder $folder" }
    }

    private fun isOldMessage(backendFolder: BackendFolder, message: Pop3Message): Boolean {
        return message.olderThan(getLatestOldMessageSeenTime(backendFolder))
    }

    @Suppress("LongParameterList")
    @Throws(MessagingException::class)
    private fun downloadLargeMessages(
        syncConfig: SyncConfig,
        remoteFolder: Pop3Folder,
        backendFolder: BackendFolder,
        largeMessages: MutableList<Pop3Message>,
        progress: AtomicInteger,
        newMessages: AtomicInteger,
        todo: Int,
        fp: FetchProfile?,
        listener: SyncListener,
    ) {
        val folder = remoteFolder.serverId

        logger.debug(tag = TAG) { "SYNC: Fetching large messages for folder $folder" }

        val maxDownloadSize = syncConfig.maximumAutoDownloadMessageSize
        remoteFolder.fetch(largeMessages, fp, null, maxDownloadSize)
        for (message in largeMessages) {
            downloadSaneBody(syncConfig, remoteFolder, backendFolder, message)

            val messageServerId = message.uid
            logger.verbose(tag = TAG) {
                "About to notify listeners that we got a new large message $accountName:$folder:$messageServerId"
            }

            // Update the listener with what we've found
            progress.incrementAndGet()

            // TODO do we need to re-fetch this here?
            val flags: Set<Flag> = backendFolder.getMessageFlags(messageServerId)
            // Increment the number of "new messages" if the newly downloaded message is
            // not marked as read.
            if (!flags.contains(Flag.SEEN)) {
                newMessages.incrementAndGet()
            }

            listener.syncProgress(folderServerId = folder, completed = progress.get(), total = todo)

            val isOldMessage = isOldMessage(backendFolder, message)
            listener.syncNewMessage(
                folderServerId = folder,
                messageServerId = messageServerId,
                isOldMessage = isOldMessage,
            )
        }

        logger.debug(tag = TAG) { "SYNC: Done fetching large messages for folder $folder" }
    }

    @Throws(MessagingException::class)
    private fun downloadSaneBody(
        syncConfig: SyncConfig,
        remoteFolder: Pop3Folder,
        backendFolder: BackendFolder,
        message: Pop3Message,
    ) {
        /*
         * The provider was unable to get the structure of the message, so
         * we'll download a reasonable portion of the message and mark it as
         * incomplete so the entire thing can be downloaded later if the user
         * wishes to download it.
         */
        val fp = FetchProfile()
        fp.add(FetchProfile.Item.BODY_SANE)

        /*
         *  TODO a good optimization here would be to make sure that all Stores set
         *  the proper size after this fetch and compare the before and after size. If
         *  they equal we can mark this SYNCHRONIZED instead of PARTIALLY_SYNCHRONIZED
         */
        val maxDownloadSize = syncConfig.maximumAutoDownloadMessageSize
        remoteFolder.fetch(mutableListOf<Pop3Message?>(message), fp, null, maxDownloadSize)

        var completeMessage = false
        // Certain (POP3) servers give you the whole message even when you ask for only the first x Kb
        if (!message.isSet(Flag.X_DOWNLOADED_FULL)) {
            /*
             * Mark the message as fully downloaded if the message size is smaller than
             * the account's autodownload size limit, otherwise mark as only a partial
             * download.  This will prevent the system from downloading the same message
             * twice.
             *
             * If there is no limit on autodownload size, that's the same as the message
             * being smaller than the max size
             */
            if (syncConfig.maximumAutoDownloadMessageSize == 0 ||
                message.size < syncConfig.maximumAutoDownloadMessageSize
            ) {
                completeMessage = true
            }
        }

        // Store the updated message locally
        runBlocking {
            if (completeMessage) {
                backendFolder.saveMessage(message, MessageDownloadState.FULL)
            } else {
                backendFolder.saveMessage(message, MessageDownloadState.PARTIAL)
            }
        }
    }

    companion object {
        private const val EXTRA_LATEST_OLD_MESSAGE_SEEN_TIME = "latestOldMessageSeenTime"
    }
}
