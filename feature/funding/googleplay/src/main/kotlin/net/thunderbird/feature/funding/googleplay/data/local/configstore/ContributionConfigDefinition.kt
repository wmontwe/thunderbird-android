package net.thunderbird.feature.funding.googleplay.data.local.configstore

import net.thunderbird.core.configstore.ConfigId
import net.thunderbird.core.configstore.ConfigKey
import net.thunderbird.core.configstore.ConfigMapper
import net.thunderbird.feature.funding.googleplay.data.FundingDataContract.Local

/**
 * Definition for contribution configuration.
 */
internal class ContributionConfigDefinition(
    override val version: Int = 1,
    override val id: ConfigId = ConfigId(backend = "funding", feature = "contribution"),
    override val mapper: ConfigMapper<ContributionConfig>,
    override val defaultValue: ContributionConfig = ContributionConfig.DEFAULT,
    override val keys: List<ConfigKey<*>> = listOf(
        ContributionConfigKeys.LAST_PURCHASED_CONTRIBUTION,
    ),
    override val migration: ContributionConfigMigration,
) : Local.ContributionConfigDefinition
