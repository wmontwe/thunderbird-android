package com.fsck.k9.backend.jmap

import net.thunderbird.components.core.logging.Logger
import net.thunderbird.core.common.mail.Flag
import rs.ltt.jmap.client.JmapClient
import rs.ltt.jmap.common.entity.filter.EmailFilterCondition
import rs.ltt.jmap.common.method.call.email.QueryEmailMethodCall
import rs.ltt.jmap.common.method.call.email.SetEmailMethodCall
import rs.ltt.jmap.common.method.response.email.QueryEmailMethodResponse
import rs.ltt.jmap.common.method.response.email.SetEmailMethodResponse
import rs.ltt.jmap.common.util.Patches

private const val TAG = "CommandSetFlag"

class CommandSetFlag(
    private val jmapClient: JmapClient,
    private val accountId: String,
    private val logger: Logger,
) {
    fun setFlag(messageServerIds: List<String>, flag: Flag, newState: Boolean) {
        if (newState) {
            logger.verbose(tag = TAG) { "Setting flag $flag for messages $messageServerIds" }
        } else {
            logger.verbose(tag = TAG) { "Removing flag $flag for messages $messageServerIds" }
        }

        val keyword = flag.toKeyword()
        val keywordsPatch = if (newState) {
            Patches.set("keywords/$keyword", true)
        } else {
            Patches.remove("keywords/$keyword")
        }

        val session = jmapClient.session.get()
        val maxObjectsInSet = session.maxObjectsInSet

        messageServerIds.chunked(maxObjectsInSet).forEach { emailIds ->
            val updates = emailIds.map { emailId ->
                emailId to keywordsPatch
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

    fun markAllAsRead(folderServerId: String) {
        logger.debug(tag = TAG) { "Marking all messages in $folderServerId as read" }

        val keywordsPatch = Patches.set("keywords/\$seen", true)

        val session = jmapClient.session.get()
        val limit = minOf(MAX_CHUNK_SIZE, session.maxObjectsInSet).toLong()

        do {
            logger.verbose(tag = TAG) { "Trying to mark up to $limit messages in $folderServerId as read" }

            val queryEmailCall = jmapClient.call(
                QueryEmailMethodCall.builder()
                    .accountId(accountId)
                    .filter(
                        EmailFilterCondition.builder()
                            .inMailbox(folderServerId)
                            .notKeyword("\$seen")
                            .build(),
                    )
                    .calculateTotal(true)
                    .limit(limit)
                    .build(),
            )

            val queryEmailResponse = queryEmailCall.getMainResponseBlocking<QueryEmailMethodResponse>()
            val numberOfReturnedEmails = queryEmailResponse.ids.size
            val totalNumberOfEmails = queryEmailResponse.total ?: error("Server didn't return property 'total'")

            if (numberOfReturnedEmails == 0) {
                logger.verbose(tag = TAG) { "There were no messages in $folderServerId to mark as read" }
            } else {
                val updates = queryEmailResponse.ids.map { emailId ->
                    emailId to keywordsPatch
                }.toMap()

                val setEmailCall = jmapClient.call(
                    SetEmailMethodCall.builder()
                        .accountId(accountId)
                        .update(updates)
                        .build(),
                )

                setEmailCall.getMainResponseBlocking<SetEmailMethodResponse>()

                logger.verbose(tag = TAG) { "Marked $numberOfReturnedEmails messages in $folderServerId as read" }
            }
        } while (totalNumberOfEmails > numberOfReturnedEmails)
    }

    private fun Flag.toKeyword(): String = when (this) {
        Flag.SEEN -> "\$seen"
        Flag.FLAGGED -> "\$flagged"
        Flag.DRAFT -> "\$draft"
        Flag.ANSWERED -> "\$answered"
        Flag.FORWARDED -> "\$forwarded"
        else -> error("Unsupported flag: $name")
    }
}
