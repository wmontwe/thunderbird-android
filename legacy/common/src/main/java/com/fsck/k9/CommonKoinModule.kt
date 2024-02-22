package com.fsck.k9

import android.os.Build
import app.k9mail.core.common.net.ssl.installTrustedCertificates
import app.k9mail.core.featureflag.FeatureFlagFactory
import app.k9mail.core.featureflag.FeatureFlagProvider
import app.k9mail.core.featureflag.InMemoryFeatureFlagProvider
import app.k9mail.feature.widget.message.list.messageListWidgetModule
import app.k9mail.feature.widget.unread.UnreadWidgetUpdateListener
import app.k9mail.feature.widget.unread.unreadWidgetModule
import com.fsck.k9.account.newAccountModule
import com.fsck.k9.backends.backendsModule
import com.fsck.k9.controller.ControllerExtension
import com.fsck.k9.crypto.EncryptionExtractor
import com.fsck.k9.crypto.openpgp.OpenPgpEncryptionExtractor
import com.fsck.k9.feature.featureLauncherModule
import com.fsck.k9.feature.featureModule
import com.fsck.k9.featureflag.InMemoryFeatureFlagFactory
import com.fsck.k9.net.networkModule
import com.fsck.k9.net.ssl.CompatTrustedCertificateProvider
import com.fsck.k9.notification.notificationModule
import com.fsck.k9.preferences.K9StoragePersister
import com.fsck.k9.preferences.StoragePersister
import com.fsck.k9.resources.resourcesModule
import com.fsck.k9.storage.storageModule
import org.koin.core.qualifier.named
import org.koin.dsl.module

val commonAppModule = module {
    single {
        MessagingListenerProvider(
            listOf(
                get<UnreadWidgetUpdateListener>(),
            ),
        )
    }
    single(named("controllerExtensions")) { emptyList<ControllerExtension>() }
    single<EncryptionExtractor> { OpenPgpEncryptionExtractor.newInstance() }
    single<StoragePersister> { K9StoragePersister(get()) }
    single<FeatureFlagProvider> {
        InMemoryFeatureFlagProvider(
            featureFlagFactory = get(),
        )
    }
    single<OkHttpClient> {
        val builder = OkHttpClient.Builder()

        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.N) {
            builder.installTrustedCertificates(
                trustedCertificateProvider = CompatTrustedCertificateProvider(),
            )
        }

        builder.build()
    }
}

val commonAppModules = listOf(
    commonAppModule,
    messageListWidgetModule,
    networkModule,
    unreadWidgetModule,
    notificationModule,
    resourcesModule,
    backendsModule,
    storageModule,
    newAccountModule,
    featureLauncherModule,
)
