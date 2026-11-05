package com.fsck.k9.backend.jmap

import net.thunderbird.components.core.logging.Logger
import rs.ltt.jmap.client.JmapClient
import rs.ltt.jmap.common.Request.Invocation.ResultReference
import rs.ltt.jmap.common.entity.filter.EmailFilterCondition
import rs.ltt.jmap.common.method.call.email.QueryEmailMethodCall
import rs.ltt.jmap.common.method.call.email.SetEmailMethodCall
import rs.ltt.jmap.common.method.response.email.QueryEmailMethodResponse
import rs.ltt.jmap.common.method.response.email.SetEmailMethodResponse

private const val TAG = "CommandDelete"

class CommandDelete(
    private val jmapClient: JmapClient,
    private val accountId: String,
    private val logger: Logger,
) {
    fun deleteMessages(messageServerIds: List<String>) {
        logger.verbose(tag = TAG) { "Deleting messages $messageServerIds" }

        val session = jmapClient.session.get()
        val maxObjectsInSet = session.maxObjectsInSet

        messageServerIds.chunked(maxObjectsInSet).forEach { emailIds ->
            val setEmailCall = jmapClient.call(
                SetEmailMethodCall.builder()
                    .accountId(accountId)
                    .destroy(emailIds.toTypedArray())
                    .build(),
            )

            setEmailCall.getMainResponseBlocking<SetEmailMethodResponse>()
        }
    }

    fun deleteAllMessages(folderServerId: String) {
        logger.debug(tag = TAG) { "Deleting all messages from $folderServerId" }

        val session = jmapClient.session.get()
        val limit = session.maxObjectsInSet.coerceAtMost(MAX_CHUNK_SIZE).toLong()

        do {
            logger.verbose(tag = TAG) { "Trying to delete up to $limit messages from $folderServerId" }
            val multiCall = jmapClient.newMultiCall()

            val queryEmailCall = multiCall.call(
                QueryEmailMethodCall.builder()
                    .accountId(accountId)
                    .filter(EmailFilterCondition.builder().inMailbox(folderServerId).build())
                    .calculateTotal(true)
                    .limit(limit)
                    .build(),
            )

            val setEmailCall = multiCall.call(
                SetEmailMethodCall.builder()
                    .accountId(accountId)
                    .destroyReference(queryEmailCall.createResultReference(ResultReference.Path.IDS))
                    .build(),
            )

            multiCall.execute()

            val queryEmailResponse = queryEmailCall.getMainResponseBlocking<QueryEmailMethodResponse>()
            val numberOfReturnedEmails = queryEmailResponse.ids.size
            val totalNumberOfEmails = queryEmailResponse.total ?: error("Server didn't return property 'total'")

            setEmailCall.getMainResponseBlocking<SetEmailMethodResponse>()

            logger.verbose(tag = TAG) { "Deleted $numberOfReturnedEmails messages from $folderServerId" }
        } while (totalNumberOfEmails > numberOfReturnedEmails)
    }
}
