package app.k9mail.feature.onboarding.migration.thunderbird

import androidx.compose.runtime.Composable
import net.thunderbird.components.core.logging.DefaultLogger
import net.thunderbird.components.core.logging.LogLevel
import net.thunderbird.components.core.logging.console.ConsoleLogSink
import net.thunderbird.components.ui.bolt.atom.Surface
import net.thunderbird.components.ui.bolt.common.annotation.PreviewDevices
import net.thunderbird.components.ui.bolt.theme.thunderbird.ThunderbirdBoltTheme
import net.thunderbird.core.common.provider.BrandNameProvider

@Composable
@PreviewDevices
internal fun TbOnboardingMigrationScreenPreview() {
    ThunderbirdBoltTheme {
        Surface {
            TbOnboardingMigrationScreen(
                onThundermailClick = {},
                onQrCodeScan = {},
                onAddAccount = {},
                onImport = {},
                brandNameProvider = object : BrandNameProvider {
                    override val brandName: String = "Thunderbird"
                },
                logger = DefaultLogger(sink = ConsoleLogSink(level = LogLevel.VERBOSE)),
            )
        }
    }
}
