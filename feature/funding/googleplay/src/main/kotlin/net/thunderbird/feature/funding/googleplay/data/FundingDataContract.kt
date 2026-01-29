package net.thunderbird.feature.funding.googleplay.data

import android.app.Activity
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import net.thunderbird.core.outcome.Outcome
import net.thunderbird.feature.funding.googleplay.domain.FundingDomainContract.ContributionError
import net.thunderbird.feature.funding.googleplay.domain.entity.Contribution
import net.thunderbird.feature.funding.googleplay.domain.entity.OneTimeContribution
import net.thunderbird.feature.funding.googleplay.domain.entity.RecurringContribution
import com.android.billingclient.api.BillingResult as GoogleBillingResult

internal interface FundingDataContract {

    interface Mapper {
        interface Product {
            fun mapToContribution(product: ProductDetails): Contribution

            fun mapToOneTimeContribution(product: ProductDetails): OneTimeContribution
            fun mapToRecurringContribution(product: ProductDetails): RecurringContribution
        }

        interface BillingResult {
            suspend fun <T> mapToOutcome(
                billingResult: GoogleBillingResult,
                transformSuccess: suspend () -> T,
            ): Outcome<T, ContributionError>
        }
    }

    interface BillingClient {

        /**
         * Flow that emits the last purchased contribution.
         */
        val purchasedContribution: StateFlow<Outcome<Contribution?, ContributionError>>

        /**
         * Connect to the billing service.
         *
         * @param onConnected Callback to be invoked when the billing service is connected.
         */
        suspend fun <T> connect(onConnected: suspend () -> Outcome<T, ContributionError>): Outcome<T, ContributionError>

        /**
         * Disconnect from the billing service.
         */
        fun disconnect()

        /**
         * Load one-time contributions.
         */
        fun loadOneTimeContributions(
            productIds: List<String>,
        ): Flow<Outcome<List<OneTimeContribution>, ContributionError>>

        /**
         * Load recurring contributions.
         */
        fun loadRecurringContributions(
            productIds: List<String>,
        ): Flow<Outcome<List<RecurringContribution>, ContributionError>>

        /**
         * Load purchased one-time contributions.
         */
        fun loadPurchasedOneTimeContributions(): Flow<Outcome<List<OneTimeContribution>, ContributionError>>

        /**
         *  Load purchased recurring contributions.
         */
        fun loadPurchasedRecurringContributions(): Flow<Outcome<List<RecurringContribution>, ContributionError>>

        /**
         * Load the most recent one-time contribution.
         */
        fun loadPurchasedOneTimeContributionHistory(): Flow<Outcome<OneTimeContribution?, ContributionError>>

        /**
         * Purchase a contribution.
         */
        suspend fun purchaseContribution(
            activity: Activity,
            contribution: Contribution,
        ): Outcome<Unit, ContributionError>
    }

    interface Remote {
        interface GoogleBillingClientProvider {
            val current: com.android.billingclient.api.BillingClient

            /**
             * Set the listener to be notified of purchase updates.
             */
            fun setPurchasesUpdatedListener(listener: PurchasesUpdatedListener)

            /**
             * Disconnect from the billing service and clear the instance.
             */
            fun clear()
        }

        interface GoogleBillingPurchaseHandler {
            suspend fun handlePurchases(
                clientProvider: GoogleBillingClientProvider,
                purchases: List<Purchase>,
            ): List<Contribution>

            suspend fun handleOneTimePurchases(
                clientProvider: GoogleBillingClientProvider,
                purchases: List<Purchase>,
            ): List<OneTimeContribution>

            suspend fun handleRecurringPurchases(
                clientProvider: GoogleBillingClientProvider,
                purchases: List<Purchase>,
            ): List<RecurringContribution>
        }
    }
}
