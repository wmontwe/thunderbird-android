package app.k9mail.core.android.common.net.ssl

import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.X509TrustManager

object CustomSslSocketFactory {
    fun create(
        trustManager: X509TrustManager,
    ): SSLSocketFactory {
        val sslContext = SSLContext.getInstance("TLS")
        sslContext.init(null, arrayOf(trustManager), null)
        return sslContext.socketFactory
    }
}
