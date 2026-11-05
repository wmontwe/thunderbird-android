package com.fsck.k9.backend.jmap

import net.thunderbird.components.core.logging.Logger
import rs.ltt.jmap.client.JmapClient
import rs.ltt.jmap.common.method.call.email.SetEmailMethodCall
import rs.ltt.jmap.common.method.response.email.SetEmailMethodResponse
import rs.ltt.jmap.common.util.Patches

private const val TAG = "CommandMove"

class CommandMove(
    private val jmapClient: JmapClient,
    private val accountId: String,
    private val logger: Logger,
) {
    fun moveMessages(targetFolderServerId: String, messageServerIds: List<String>) {
        logger.verbose(tag = TAG) { "Moving ${messageServerIds.size} messages to $targetFolderServerId" }

        val mailboxPatch = Patches.set("mailboxIds", mapOf(targetFolderServerId to true))
        updateEmails(messageServerIds, mailboxPatch)
    }

    fun moveMessagesAndMarkAsRead(targetFolderServerId: String, messageServerIds: List<String>) {
        logger.verbose(tag = TAG) {
            "Moving ${messageServerIds.size} messages to $targetFolderServerId and marking them as read"
        }

        val mailboxPatch = Patches.builder()
            .set("mailboxIds", mapOf(targetFolderServerId to true))
            .set("keywords/\$seen", true)
            .build()
        updateEmails(messageServerIds, mailboxPatch)
    }

    fun copyMessages(targetFolderServerId: String, messageServerIds: List<String>) {
        logger.verbose(tag = TAG) { "Copying ${messageServerIds.size} messages to $targetFolderServerId" }

        val mailboxPatch = Patches.set("mailboxIds/$targetFolderServerId", true)
        updateEmails(messageServerIds, mailboxPatch)
    }

    private fun updateEmails(messageServerIds: List<String>, patch: Map<String, Any>?) {
        val session = jmapClient.session.get()
        val maxObjectsInSet = session.maxObjectsInSet

        messageServerIds.chunked(maxObjectsInSet).forEach { emailIds ->
            val updates = emailIds.map { emailId ->
                emailId to patch
            }.toMap()

            val setEmailCall = jmapClient.call(
                SetEmailMethodCall.builder()
                    .accountId(accountId)
                    .update(updates)
                    .build(),
            )

            setEmailCall.getMainResponseBlocking<SetEmailMethodResponse>()
        }
    }
}
