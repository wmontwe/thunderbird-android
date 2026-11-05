package com.fsck.k9.backend.imap

import com.fsck.k9.mail.power.PowerManager
import com.fsck.k9.mail.store.imap.IdleRefreshManager
import com.fsck.k9.mail.store.imap.IdleRefreshTimeoutProvider
import com.fsck.k9.mail.store.imap.IdleResult
import com.fsck.k9.mail.store.imap.ImapFolderIdler
import com.fsck.k9.mail.store.imap.ImapStore
import kotlin.concurrent.thread
import net.thunderbird.components.core.logging.Logger

private const val TAG = "ImapFolderPusher"

/**
 * Listens for changes to an IMAP folder in a dedicated thread.
 */
class ImapFolderPusher(
    private val imapStore: ImapStore,
    private val powerManager: PowerManager,
    private val idleRefreshManager: IdleRefreshManager,
    private val callback: ImapPusherCallback,
    private val accountName: String,
    private val folderServerId: String,
    private val idleRefreshTimeoutProvider: IdleRefreshTimeoutProvider,
    private val logger: Logger,
) {
    @Volatile
    private var folderIdler: ImapFolderIdler? = null

    @Volatile
    private var stopPushing = false

    fun start() {
        logger.verbose(tag = TAG) { "Starting ImapFolderPusher for $accountName / $folderServerId" }

        thread(name = "ImapFolderPusher-$accountName-$folderServerId") {
            logger.verbose(tag = TAG) { "Starting ImapFolderPusher thread for $accountName / $folderServerId" }

            runPushLoop()

            logger.verbose(tag = TAG) { "Exiting ImapFolderPusher thread for $accountName / $folderServerId" }
        }
    }

    fun refresh() {
        logger.verbose(tag = TAG) { "Refreshing ImapFolderPusher for $accountName / $folderServerId" }

        folderIdler?.refresh()
    }

    fun stop() {
        logger.verbose(tag = TAG) { "Stopping ImapFolderPusher for $accountName / $folderServerId" }

        stopPushing = true
        folderIdler?.stop()
    }

    private fun runPushLoop() {
        val wakeLock = powerManager.newWakeLock("ImapFolderPusher-$accountName-$folderServerId")
        wakeLock.acquire()

        performInitialSync()

        val folderIdler = ImapFolderIdler.create(
            idleRefreshManager,
            wakeLock,
            imapStore,
            folderServerId,
            idleRefreshTimeoutProvider,
        ).also {
            folderIdler = it
        }

        try {
            while (!stopPushing) {
                when (folderIdler.idle()) {
                    IdleResult.SYNC -> {
                        callback.onPushEvent(folderServerId)
                    }
                    IdleResult.STOPPED -> {
                        // ImapFolderIdler only stops when we ask it to.
                        // But it can't hurt to make extra sure we exit the loop.
                        stopPushing = true
                    }
                    IdleResult.NOT_SUPPORTED -> {
                        stopPushing = true
                        callback.onPushNotSupported()
                    }
                }
            }
        } catch (e: Exception) {
            logger.verbose(tag = TAG, throwable = e) { "Exception in ImapFolderPusher" }

            this.folderIdler = null
            callback.onPushError(folderServerId, e)
        }

        wakeLock.release()
    }

    private fun performInitialSync() {
        callback.onPushEvent(folderServerId)
    }
}
