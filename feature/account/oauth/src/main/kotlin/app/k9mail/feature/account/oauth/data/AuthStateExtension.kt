package app.k9mail.feature.account.oauth.data

import app.k9mail.feature.account.common.domain.entity.AuthorizationState
import net.openid.appauth.AuthState
import net.thunderbird.components.core.logging.Logger
import org.json.JSONException

private const val TAG = "AuthStateExtension"

fun AuthState.toAuthorizationState(logger: Logger): AuthorizationState {
    return try {
        AuthorizationState(value = jsonSerializeString())
    } catch (e: JSONException) {
        logger.error(tag = TAG, throwable = e) { "Error serializing AuthorizationState" }
        AuthorizationState()
    }
}

fun AuthorizationState.toAuthState(logger: Logger): AuthState {
    return try {
        value?.let { AuthState.jsonDeserialize(it) } ?: AuthState()
    } catch (e: JSONException) {
        logger.error(tag = TAG, throwable = e) { "Error deserializing AuthorizationState" }
        AuthState()
    }
}
