package com.fsck.k9.net

import android.os.Build
import app.k9mail.core.common.net.ssl.installTrustedCertificates
import com.fsck.k9.net.ssl.CompatTrustedCertificateProvider
import okhttp3.OkHttpClient

fun createOkHttpClient(): OkHttpClient {
    val builder = OkHttpClient.Builder()

    if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.N) {
        builder.installTrustedCertificates(
            trustedCertificateProvider = CompatTrustedCertificateProvider(),
        )
    }

    return builder.build()
}
