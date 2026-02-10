package net.thunderbird.feature.funding.googleplay.data.local.configstore

import net.thunderbird.core.configstore.BaseConfigStore
import net.thunderbird.core.configstore.ConfigDefinition
import net.thunderbird.core.configstore.backend.ConfigBackendProvider
import net.thunderbird.feature.funding.googleplay.data.FundingDataContract.Local

internal class ContributionConfigStore(
    provider: ConfigBackendProvider,
    definition: ConfigDefinition<ContributionConfig>,
) : BaseConfigStore<ContributionConfig>(
    provider = provider,
    definition = definition,
),
    Local.ContributionConfigStore
