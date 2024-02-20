package app.k9mail.core.android.common.net.ssl

import android.annotation.SuppressLint
import java.security.cert.CertificateException
import java.security.cert.X509Certificate
import javax.net.ssl.X509TrustManager

/**
 * A [X509TrustManager] that checks .
 */
@SuppressLint("CustomX509TrustManager")
class CustomTrustManager(
    private val trustManagers: List<X509TrustManager>,
) : X509TrustManager {

    override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {
        var certificateException = CertificateException("No trust manager found for client")
        trustManagers.forEach { trustManager ->
            try {
                trustManager.checkClientTrusted(chain, authType)
                return
            } catch (exception: CertificateException) {
                certificateException = exception
            }
        }

        throw certificateException
    }

    override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {
        var certificateException = CertificateException("No trust manager found for server")
        trustManagers.forEach { trustManager ->
            try {
                trustManager.checkServerTrusted(chain, authType)
                return
            } catch (exception: CertificateException) {
                certificateException = exception
            }
        }
        throw certificateException
    }

    override fun getAcceptedIssuers(): Array<X509Certificate> {
        val certificates = mutableListOf<X509Certificate>()
        for (trustManager in trustManagers) {
            certificates.addAll(listOf(*trustManager.acceptedIssuers))
        }
        return certificates.toTypedArray<X509Certificate>()
    }
}
