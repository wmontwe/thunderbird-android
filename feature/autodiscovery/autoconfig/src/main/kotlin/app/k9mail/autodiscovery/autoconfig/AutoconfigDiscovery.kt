package app.k9mail.autodiscovery.autoconfig

import app.k9mail.autodiscovery.api.AutoDiscovery
import app.k9mail.autodiscovery.api.AutoDiscoveryRunnable
import net.thunderbird.components.core.logging.Logger
import net.thunderbird.core.common.mail.EmailAddress
import net.thunderbird.core.common.mail.toDomain
import okhttp3.OkHttpClient

class AutoconfigDiscovery internal constructor(
    private val urlProvider: AutoconfigUrlProvider,
    private val autoconfigFetcher: AutoconfigFetcher,
) : AutoDiscovery {

    override fun initDiscovery(email: EmailAddress): List<AutoDiscoveryRunnable> {
        val domain = email.domain.toDomain()

        val autoconfigUrls = urlProvider.getAutoconfigUrls(domain, email)

        return autoconfigUrls.map { autoconfigUrl ->
            AutoDiscoveryRunnable {
                autoconfigFetcher.fetchAutoconfig(autoconfigUrl, email)
            }
        }
    }
}

fun createProviderAutoconfigDiscovery(
    okHttpClient: OkHttpClient,
    config: AutoconfigUrlConfig,
    logger: Logger,
): AutoconfigDiscovery {
    val urlProvider = ProviderAutoconfigUrlProvider(config)
    return createAutoconfigDiscovery(okHttpClient, urlProvider, logger)
}

fun createIspDbAutoconfigDiscovery(okHttpClient: OkHttpClient, logger: Logger): AutoconfigDiscovery {
    val urlProvider = IspDbAutoconfigUrlProvider()
    return createAutoconfigDiscovery(okHttpClient, urlProvider, logger)
}

private fun createAutoconfigDiscovery(
    okHttpClient: OkHttpClient,
    urlProvider: AutoconfigUrlProvider,
    logger: Logger,
): AutoconfigDiscovery {
    val autoconfigFetcher = RealAutoconfigFetcher(
        fetcher = OkHttpFetcher(okHttpClient),
        parser = SuspendableAutoconfigParser(RealAutoconfigParser(logger)),
        logger = logger,
    )
    return AutoconfigDiscovery(urlProvider, autoconfigFetcher)
}
