package net.thunderbird.feature.funding.googleplay.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import net.thunderbird.core.outcome.Outcome
import net.thunderbird.core.outcome.mapSuccess
import net.thunderbird.feature.funding.googleplay.domain.FundingDomainContract
import net.thunderbird.feature.funding.googleplay.domain.FundingDomainContract.ContributionError
import net.thunderbird.feature.funding.googleplay.domain.entity.Contribution
import net.thunderbird.feature.funding.googleplay.domain.entity.OneTimeContribution
import net.thunderbird.feature.funding.googleplay.domain.entity.RecurringContribution

internal class ContributionRepository(
    private val billingClient: FundingDataContract.BillingClient,
    private val contributionIdProvider: FundingDomainContract.ContributionIdProvider,
) : FundingDomainContract.ContributionRepository {

    override fun getAllOneTime(): Flow<Outcome<List<OneTimeContribution>, ContributionError>> =
        billingClient.loadOneTimeContributions(
            productIds = contributionIdProvider.oneTimeContributionIds,
        ).map { it.mapSuccess { contributions -> contributions.sortedByDescending { it.price } } }

    override fun getAllRecurring(): Flow<Outcome<List<RecurringContribution>, ContributionError>> =
        billingClient.loadRecurringContributions(
            productIds = contributionIdProvider.recurringContributionIds,
        ).map { it.mapSuccess { contributions -> contributions.sortedByDescending { it.price } } }

    override fun getAllPurchased(): Flow<Outcome<List<Contribution>, ContributionError>> =
        billingClient.loadPurchasedRecurringContributions().flatMapLatest { recurringResult ->
            if (recurringResult is Outcome.Success) {
                val recurringContributions = recurringResult.data
                if (recurringContributions.isEmpty()) {
                    billingClient.loadPurchasedOneTimeContributionHistory().map { result ->
                        result.mapSuccess { contribution ->
                            if (contribution != null) {
                                listOf(contribution)
                            } else {
                                emptyList()
                            }
                        }
                    }
                } else {
                    flow { emit(Outcome.success(recurringContributions.sortedByDescending { it.price })) }
                }
            } else {
                flow { emit(recurringResult as Outcome<List<Contribution>, ContributionError>) }
            }
        }
}
