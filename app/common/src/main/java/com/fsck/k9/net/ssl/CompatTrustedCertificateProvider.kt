package com.fsck.k9.net.ssl

import app.k9mail.core.common.net.ssl.TrustedCertificateProvider
import app.k9mail.core.common.net.ssl.decodeCertificatePem
import java.security.cert.X509Certificate

/**
 * A [TrustedCertificateProvider] that provides missing certificates for Android 7 and earlier.
 */
class CompatTrustedCertificateProvider : TrustedCertificateProvider {
    override fun getCertificates(): List<X509Certificate> {
        return listOf(
            TrustedCertificates.certificateIsrgRootX1.decodeCertificatePem(),
            TrustedCertificates.certificateIsrgRootX2.decodeCertificatePem(),
        )
    }
}
