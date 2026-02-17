package net.thunderbird.feature.funding.googleplay.data.remote

import android.app.Activity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import net.thunderbird.core.outcome.Outcome
import net.thunderbird.feature.funding.googleplay.data.FundingDataContract
import net.thunderbird.feature.funding.googleplay.domain.FundingDomainContract.ContributionError
import net.thunderbird.feature.funding.googleplay.domain.entity.Contribution
import net.thunderbird.feature.funding.googleplay.domain.entity.OneTimeContribution
import net.thunderbird.feature.funding.googleplay.domain.entity.RecurringContribution

internal class FakeBillingClient : FundingDataContract.Remote.BillingClient {

    // Configuration
    var connectOutcome: Outcome<Unit, ContributionError> = Outcome.success(Unit)
    var oneTimeFlow: Flow<Outcome<List<OneTimeContribution>, ContributionError>> =
        flowOf(Outcome.success(emptyList()))
    var recurringFlow: Flow<Outcome<List<RecurringContribution>, ContributionError>> =
        flowOf(Outcome.success(emptyList()))

    var purchasedOneTimeFlow: Flow<Outcome<List<OneTimeContribution>, ContributionError>> =
        flowOf(Outcome.success(emptyList()))
    var purchasedRecurringFlow: Flow<Outcome<List<RecurringContribution>, ContributionError>> =
        flowOf(Outcome.success(emptyList()))
    var purchaseHistoryFlow: Flow<Outcome<OneTimeContribution?, ContributionError>> =
        flowOf(Outcome.success(null))

    // State
    private val _purchasedContribution = MutableStateFlow<Outcome<Contribution?, ContributionError>>(
        Outcome.success(null),
    )

    override val purchasedContribution: StateFlow<Outcome<Contribution?, ContributionError>>
        get() = _purchasedContribution

    override suspend fun <T> connect(
        onConnected: suspend () -> Outcome<T, ContributionError>,
    ): Outcome<T, ContributionError> {
        return when (val result = connectOutcome) {
            is Outcome.Success -> onConnected()
            is Outcome.Failure -> Outcome.failure(result.error)
        }
    }

    override fun disconnect() {
        _purchasedContribution.value = Outcome.success(null)
    }

    override fun loadOneTimeContributions(
        productIds: List<String>,
    ): Flow<Outcome<List<OneTimeContribution>, ContributionError>> = oneTimeFlow

    override fun loadRecurringContributions(
        productIds: List<String>,
    ): Flow<Outcome<List<RecurringContribution>, ContributionError>> = recurringFlow

    override fun loadPurchasedOneTimeContributions(): Flow<Outcome<List<OneTimeContribution>, ContributionError>> =
        purchasedOneTimeFlow

    override fun loadPurchasedRecurringContributions(): Flow<Outcome<List<RecurringContribution>, ContributionError>> =
        purchasedRecurringFlow

    override fun loadPurchasedOneTimeContributionHistory(): Flow<Outcome<OneTimeContribution?, ContributionError>> =
        purchaseHistoryFlow

    override suspend fun purchaseContribution(
        activity: Activity,
        contribution: Contribution,
    ): Outcome<Unit, ContributionError> = Outcome.success(Unit)
}
