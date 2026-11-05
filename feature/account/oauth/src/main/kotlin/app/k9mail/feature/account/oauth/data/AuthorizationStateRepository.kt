package app.k9mail.feature.account.oauth.data

import app.k9mail.feature.account.common.domain.entity.AuthorizationState
import app.k9mail.feature.account.oauth.domain.AccountOAuthDomainContract
import net.thunderbird.components.core.logging.Logger

class AuthorizationStateRepository(
    private val logger: Logger,
) : AccountOAuthDomainContract.AuthorizationStateRepository {
    override fun isAuthorized(authorizationState: AuthorizationState): Boolean {
        val authState = authorizationState.toAuthState(logger)

        return authState.isAuthorized
    }
}
