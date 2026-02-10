package net.thunderbird.feature.funding.googleplay.data.local

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import net.thunderbird.core.outcome.Outcome
import net.thunderbird.feature.funding.googleplay.data.FundingDataContract.Local
import net.thunderbird.feature.funding.googleplay.data.local.configstore.ContributionConfig
import net.thunderbird.feature.funding.googleplay.data.local.configstore.ContributionPurchase
import org.junit.Test

class LocalContributionPurchaseDataSourceTest {

    private val fakeConfigStore = FakeContributionConfigStore()
    private val testSubject = LocalContributionPurchaseDataSource(fakeConfigStore)

    @Test
    fun `get should return last purchased contribution from config store`() = runTest {
        val purchase = createContributionPurchase()
        fakeConfigStore.setConfig(ContributionConfig(lastPurchasedContribution = purchase))

        val result = testSubject.get().first()

        when (result) {
            is Outcome.Success -> assertThat(result.data).isEqualTo(purchase)
            else -> throw AssertionError("Expected Success, got $result")
        }
    }

    @Test
    fun `get should return null when no contribution is purchased`() = runTest {
        fakeConfigStore.setConfig(ContributionConfig(lastPurchasedContribution = null))

        val result = testSubject.get().first()

        when (result) {
            is Outcome.Success -> assertThat(result.data).isNull()
            else -> throw AssertionError("Expected Success, got $result")
        }
    }

    @Test
    fun `update should update config store with new purchase`() = runTest {
        val purchase = createContributionPurchase()

        val result = testSubject.update(purchase)

        when (result) {
            is Outcome.Success -> assertThat(
                fakeConfigStore.getCurrentConfig().lastPurchasedContribution,
            ).isEqualTo(purchase)
            else -> throw AssertionError("Expected Success, got $result")
        }
    }

    private fun createContributionPurchase() = ContributionPurchase(
        id = "id",
        title = "title",
        description = "description",
        price = 1000L,
        priceFormatted = "$1.00",
        productId = "product_id",
        orderId = "order_id",
        purchaseTimeMillis = 123456789L,
    )

    private class FakeContributionConfigStore : Local.ContributionConfigStore {
        private val _config = MutableStateFlow(ContributionConfig.DEFAULT)
        override val config = _config

        fun setConfig(config: ContributionConfig) {
            _config.value = config
        }

        fun getCurrentConfig() = _config.value

        override suspend fun update(transform: (ContributionConfig?) -> ContributionConfig) {
            _config.value = transform(_config.value)
        }

        override suspend fun clear() {
            _config.value = ContributionConfig.DEFAULT
        }
    }
}
