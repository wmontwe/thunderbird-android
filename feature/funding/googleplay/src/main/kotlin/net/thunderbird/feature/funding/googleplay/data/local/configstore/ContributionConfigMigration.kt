package net.thunderbird.feature.funding.googleplay.data.local.configstore

import net.thunderbird.core.configstore.Config
import net.thunderbird.core.configstore.ConfigMigration
import net.thunderbird.core.configstore.ConfigMigrationResult

/**
 * Migration for contribution configuration.
 *
 * Handles migration of contribution configuration between different versions.
 *
 * Please update the migration logic when necessary.
 */
class ContributionConfigMigration : ConfigMigration {
    override suspend fun migrate(
        currentVersion: Int,
        newVersion: Int,
        current: Config,
    ): ConfigMigrationResult = ConfigMigrationResult.NoOp
}
