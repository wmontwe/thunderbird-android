package net.thunderbird.feature.account.server.settings.ui.common

import androidx.activity.compose.LocalActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine
import net.thunderbird.components.core.logging.Logger
import net.thunderbird.components.core.outcome.Outcome
import org.koin.compose.koinInject

private const val TAG = "BiometricAuthenticator"

/**
 * An [Authenticator] implementation that uses Android's BiometricPrompt to authenticate the user.
 *
 *  Note: Due to limitations of [androidx.biometric.BiometricPrompt] this composable can only be used inside a
 *  [androidx.fragment.app.FragmentActivity].
 *
 * @param activity The FragmentActivity context to use for the BiometricPrompt.
 * @param title The title to display on the biometric prompt.
 * @param subtitle The subtitle to display on the biometric prompt.
 */
class BiometricAuthenticator(
    private val activity: FragmentActivity,
    private val title: String,
    private val subtitle: String,
    private val logger: Logger,
) : Authenticator {

    @Suppress("TooGenericExceptionCaught")
    override suspend fun authenticate(): Outcome<Unit, AuthenticationError> =
        suspendCancellableCoroutine { continuation ->
            val authenticationCallback = object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    if (continuation.isActive) continuation.resume(Outcome.Success(Unit))
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    val error: AuthenticationError = when (errorCode) {
                        BiometricPrompt.ERROR_HW_NOT_PRESENT,
                        BiometricPrompt.ERROR_NO_DEVICE_CREDENTIAL,
                        BiometricPrompt.ERROR_NO_BIOMETRICS,
                        -> AuthenticationError.NotAvailable

                        else -> AuthenticationError.Failed
                    }
                    if (continuation.isActive) continuation.resume(Outcome.Failure(error))
                }
            }

            val promptInfo = BiometricPrompt.PromptInfo.Builder()
                .setAllowedAuthenticators(
                    BiometricManager.Authenticators.BIOMETRIC_STRONG or
                        BiometricManager.Authenticators.BIOMETRIC_WEAK or
                        BiometricManager.Authenticators.DEVICE_CREDENTIAL,
                )
                .setTitle(title)
                .setSubtitle(subtitle)
                .build()

            val executor = ContextCompat.getMainExecutor(activity)
            try {
                BiometricPrompt(activity, executor, authenticationCallback).authenticate(promptInfo)
            } catch (e: Exception) {
                logger.error(tag = TAG, throwable = e) { "Failed to start biometric authentication" }
                if (continuation.isActive) continuation.resume(Outcome.Failure(AuthenticationError.UnableToStart))
            }

            continuation.invokeOnCancellation {
                // No explicit cancellation support for BiometricPrompt
            }
        }
}

/**
 * Creates and remembers a [BiometricAuthenticator].
 *
 * @param title The title to display on the biometric prompt.
 * @param subtitle The subtitle to display on the biometric prompt.
 */
@Composable
fun rememberBiometricAuthenticator(
    title: String,
    subtitle: String,
): Authenticator {
    val fragmentActivity = LocalActivity.current as? FragmentActivity
    if (fragmentActivity == null) {
        // Fallback for previews and other non-FragmentActivity contexts, which also have no Koin to inject from
        return remember { Authenticator { Outcome.Failure(AuthenticationError.UnableToStart) } }
    }

    val logger: Logger = koinInject()
    return remember(fragmentActivity, title, subtitle, logger) {
        BiometricAuthenticator(
            activity = fragmentActivity,
            title = title,
            subtitle = subtitle,
            logger = logger,
        )
    }
}
