package com.fsck.k9.backend

import com.fsck.k9.backend.api.Backend
import com.fsck.k9.mail.ServerSettings
import java.util.concurrent.CopyOnWriteArraySet
import net.thunderbird.core.android.account.LegacyAccountDto
import net.thunderbird.feature.account.AccountId

class BackendManager(
    private val backendFactories: Map<String, BackendFactory>,
) {
    private val backendCache = mutableMapOf<AccountId, BackendContainer>()
    private val listeners = CopyOnWriteArraySet<BackendChangedListener>()

    fun getBackend(account: LegacyAccountDto): Backend {
        val newBackend = synchronized(backendCache) {
            val container = backendCache[account.id]
            if (container != null && isBackendStillValid(container, account)) {
                return container.backend
            }

            createBackend(account).also { backend ->
                backendCache[account.id] = BackendContainer(
                    backend,
                    account.incomingServerSettings,
                    account.outgoingServerSettings,
                )
            }
        }

        notifyListeners(account.id)

        return newBackend
    }

    private fun isBackendStillValid(container: BackendContainer, account: LegacyAccountDto): Boolean {
        return container.incomingServerSettings == account.incomingServerSettings &&
            container.outgoingServerSettings == account.outgoingServerSettings
    }

    fun removeBackend(accountId: AccountId) {
        synchronized(backendCache) {
            backendCache.remove(accountId)
        }

        notifyListeners(accountId)
    }

    private fun createBackend(account: LegacyAccountDto): Backend {
        val serverType = account.incomingServerSettings.type
        val backendFactory = backendFactories[serverType] ?: error("Unsupported account type")
        return backendFactory.createBackend(account)
    }

    fun addListener(listener: BackendChangedListener) {
        listeners.add(listener)
    }

    fun removeListener(listener: BackendChangedListener) {
        listeners.remove(listener)
    }

    private fun notifyListeners(accountId: AccountId) {
        for (listener in listeners) {
            listener.onBackendChanged(accountId)
        }
    }
}

private data class BackendContainer(
    val backend: Backend,
    val incomingServerSettings: ServerSettings,
    val outgoingServerSettings: ServerSettings,
)

fun interface BackendChangedListener {
    fun onBackendChanged(accountId: AccountId)
}
