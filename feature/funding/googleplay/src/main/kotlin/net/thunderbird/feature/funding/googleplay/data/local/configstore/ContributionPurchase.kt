package net.thunderbird.feature.funding.googleplay.data.local.configstore

import kotlinx.serialization.Serializable

/**
 * Represents a contribution purchase with details about the purchase.
 *
 * @param productId The product ID of the purchase
 * @param orderId The order ID of the purchase, if any
 * @param purchaseTimeMillis The purchase time in milliseconds
 */
@Serializable
data class ContributionPurchase(
    val productId: String,
    val orderId: String?,
    val purchaseTimeMillis: Long,
)
