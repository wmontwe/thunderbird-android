package net.thunderbird.feature.funding.googleplay.data.local.configstore

import com.android.billingclient.api.Purchase
import kotlinx.coroutines.flow.firstOrNull
import net.thunderbird.core.configstore.BaseConfigStore
import net.thunderbird.core.configstore.ConfigDefinition
import net.thunderbird.core.configstore.backend.ConfigBackendProvider

class ContributionConfigStore(
    provider: ConfigBackendProvider,
    definition: ConfigDefinition<ContributionConfig>,
) : BaseConfigStore<ContributionConfig>(
    provider = provider,
    definition = definition,
) {

    suspend fun record(purchase: Purchase) {
        record(
            productIds = purchase.products,
            orderId = purchase.orderId,
            purchaseTimeMillis = purchase.purchaseTime,
        )
    }

    suspend fun record(
        productIds: List<String>,
        orderId: String?,
        purchaseTimeMillis: Long,
    ) {
        update { current ->
            val purchases = current?.purchases.orEmpty().toMutableList()

            for (id in productIds) {
                val purchase = ContributionPurchase(
                    productId = id,
                    orderId = orderId,
                    purchaseTimeMillis = purchaseTimeMillis,
                )

                val index = purchases.indexOfFirst { it.productId == id }
                if (index >= 0) {
                    if (purchase.purchaseTimeMillis > purchases[index].purchaseTimeMillis) {
                        purchases[index] = purchase
                    }
                } else {
                    purchases.add(purchase)
                }
            }

            ContributionConfig(purchases = purchases)
        }
    }

    suspend fun latestFor(productIds: Set<String>): ContributionPurchase? {
        read

        return config.firstOrNull()
            ?.purchases.orEmpty()
            .filter { it.productId in productIds }
            .maxByOrNull { it.purchaseTimeMillis }
    }
}
