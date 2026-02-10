package net.thunderbird.feature.funding.googleplay.data

import net.thunderbird.feature.funding.googleplay.data.FundingDataContract.Local
import net.thunderbird.feature.funding.googleplay.data.local.LocalContributionPurchaseDataSource
import net.thunderbird.feature.funding.googleplay.data.local.configstore.ContributionConfigDefinition
import net.thunderbird.feature.funding.googleplay.data.local.configstore.ContributionConfigMapper
import net.thunderbird.feature.funding.googleplay.data.local.configstore.ContributionConfigMigration
import net.thunderbird.feature.funding.googleplay.data.local.configstore.ContributionConfigStore
import org.koin.dsl.module

internal val fundingDataModule = module {
    single<FundingDataContract.Local.ContributionConfigMapper> {
        ContributionConfigMapper(
            logger = get(),
        )
    }
    single<FundingDataContract.Local.ContributionConfigMigration> {
        ContributionConfigMigration()
    }

    single<FundingDataContract.Local.ContributionConfigDefinition> {
        ContributionConfigDefinition(
            mapper = get(),
            migration = get(),
        )
    }

    single<FundingDataContract.Local.ContributionConfigStore> {
        ContributionConfigStore(
            provider = get(),
            definition = get(),
        )
    }

    single<Local.ContributionPurchaseDataSource> {
        LocalContributionPurchaseDataSource(
            configStore = get(),
        )
    }
}
