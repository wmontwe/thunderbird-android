package net.thunderbird.feature.funding.googleplay.data.local.configstore

import kotlinx.serialization.Serializable

/**
 * Contribution config definition.
 *
 * @property purchases A list of purchases
 */
@Serializable
data class ContributionConfig(
    val purchases: List<ContributionPurchase>,
) {
    companion object {
        val DEFAULT = ContributionConfig(
            purchases = emptyList(),
        )
    }
}
