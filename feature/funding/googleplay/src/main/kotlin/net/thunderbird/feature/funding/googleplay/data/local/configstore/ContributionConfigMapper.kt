package net.thunderbird.feature.funding.googleplay.data.local.configstore

import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import net.thunderbird.core.configstore.Config
import net.thunderbird.core.configstore.ConfigMapper
import net.thunderbird.core.logging.Logger

private const val TAG = "ContributionConfigMapper"

internal class ContributionConfigMapper(
    private val logger: Logger,
) : ConfigMapper<ContributionConfig> {
    private val json = Json {
        encodeDefaults = true
        ignoreUnknownKeys = true
    }

    private val listSerializer = ListSerializer(ContributionPurchase.serializer())

    override fun toConfig(obj: ContributionConfig): Config = Config().apply {
        this[ContributionConfigKeys.PURCHASE_LIST_JSON] = json.encodeToString(listSerializer, obj.purchases)
    }

    override fun fromConfig(config: Config): ContributionConfig? {
        val jsonString = config[ContributionConfigKeys.PURCHASE_LIST_JSON] ?: return ContributionConfig.DEFAULT
        return try {
            val purchases = json.decodeFromString(listSerializer, jsonString)
            ContributionConfig(purchases = purchases)
        } catch (e: SerializationException) {
            logger.error(tag = TAG, throwable = e) {
                "Failed to deserialize PurchaseList from config: ${e.message}"
            }
            ContributionConfig.DEFAULT
        } catch (e: IllegalArgumentException) {
            logger.error(tag = TAG, throwable = e) {
                "Failed to deserialize PurchaseList from config: ${e.message}"
            }
            ContributionConfig.DEFAULT
        }
    }
}
