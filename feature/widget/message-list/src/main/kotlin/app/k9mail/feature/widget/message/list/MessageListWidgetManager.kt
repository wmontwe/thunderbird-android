package app.k9mail.feature.widget.message.list

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import app.k9mail.legacy.mailstore.MessageListChangedListener
import app.k9mail.legacy.mailstore.MessageListRepository
import com.fsck.k9.core.BuildConfig
import net.thunderbird.components.core.logging.Logger

private const val TAG = "MessageListWidgetManager"

class MessageListWidgetManager(
    private val context: Context,
    private val messageListRepository: MessageListRepository,
    private val config: MessageListWidgetConfig,
    private val logger: Logger,
) {
    private var appWidgetManager: AppWidgetManager? = null

    private var listenerAdded = false
    private val listener = MessageListChangedListener {
        onMessageListChanged()
    }

    fun init() {
        appWidgetManager = AppWidgetManager.getInstance(context)
        if (appWidgetManager == null) {
            logger.verbose(tag = TAG) { "Message list widget is not supported on this device." }
        }

        if (isAtLeastOneMessageListWidgetAdded()) {
            resetMessageListWidget()
            registerMessageListChangedListener()
        }
    }

    @Suppress("TooGenericExceptionCaught")
    private fun onMessageListChanged() {
        try {
            triggerMessageListWidgetUpdate()
        } catch (e: RuntimeException) {
            if (BuildConfig.DEBUG) {
                throw e
            } else {
                logger.error(tag = TAG, throwable = e) { "Error while updating message list widget" }
            }
        }
    }

    internal fun onWidgetAdded() {
        logger.verbose(tag = TAG) { "Message list widget added" }

        registerMessageListChangedListener()
    }

    internal fun onWidgetRemoved() {
        logger.verbose(tag = TAG) { "Message list widget removed" }

        if (!isAtLeastOneMessageListWidgetAdded()) {
            unregisterMessageListChangedListener()
        }
    }

    @Synchronized
    private fun registerMessageListChangedListener() {
        if (!listenerAdded) {
            listenerAdded = true
            messageListRepository.addListener(listener)

            logger.verbose(tag = TAG) { "Message list widget is now listening for message list changes…" }
        }
    }

    @Synchronized
    private fun unregisterMessageListChangedListener() {
        if (listenerAdded) {
            listenerAdded = false
            messageListRepository.removeListener(listener)

            logger.verbose(tag = TAG) { "Message list widget stopped listening for message list changes." }
        }
    }

    private fun isAtLeastOneMessageListWidgetAdded(): Boolean {
        return getAppWidgetIds().isNotEmpty()
    }

    private fun triggerMessageListWidgetUpdate() {
        val appWidgetIds = getAppWidgetIds()
        if (appWidgetIds.isNotEmpty()) {
            appWidgetManager?.notifyAppWidgetViewDataChanged(appWidgetIds, R.id.listView)
        }
    }

    private fun resetMessageListWidget() {
        val appWidgetIds = getAppWidgetIds()
        if (appWidgetIds.isNotEmpty()) {
            val intent = Intent(context, config.providerClass).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds)
            }

            context.sendBroadcast(intent)
        }
    }

    private fun getAppWidgetIds(): IntArray {
        val componentName = ComponentName(context, config.providerClass)
        return appWidgetManager?.getAppWidgetIds(componentName) ?: intArrayOf()
    }
}
