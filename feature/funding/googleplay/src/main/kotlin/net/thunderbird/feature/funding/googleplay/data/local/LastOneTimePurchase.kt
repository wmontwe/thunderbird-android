package net.thunderbird.feature.funding.googleplay.data.local

data class LastOneTimePurchase(
    val productId: String,
    val orderId: String?,
    val purchaseTimeMillis: Long,
)
