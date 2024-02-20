package app.k9mail.core.android.common.net.ssl

import java.security.KeyStore
import java.security.KeyStoreException
import java.security.NoSuchAlgorithmException
import java.security.cert.X509Certificate
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509KeyManager
import javax.net.ssl.X509TrustManager

/**
 * A factory for creating a [CustomTrustManager] that trusts additional certificate authorities.
 */
object CustomTrustManagerFactory {

    fun createTrustManager(
        keyStoreType: String?,
        trustedCertificates: List<X509Certificate>,
    ): X509TrustManager {
        val keyStore = createEmptyKeyStore(keyStoreType)
        trustedCertificates.forEachIndexed { index, certificate ->
            keyStore.setCertificateEntry("cert_$index", certificate)
        }

        val trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm()).apply {
            init(keyStore)
        }

        val trustManagers = trustManagerFactory.trustManagers
        check(trustManagers.size == 1 && trustManagers[0] is X509TrustManager) {
            "Expected exactly one trust manager, but got ${trustManagers.size}"
        }

        return trustManagers[0] as X509TrustManager
    }

    private fun createKeyManager(
        keyStoreType: String?,
    ): X509KeyManager {
        val keyStore = createEmptyKeyStore(keyStoreType)

        val keyFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm()).apply {
            init(keyStore, null)
        }

        val trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm()).apply {
            init(keyStore)
        }

        val trustManagers = trustManagerFactory.trustManagers
        check(trustManagers.size == 1 && trustManagers[0] is X509TrustManager) {
            "Expected exactly one trust manager, but got ${trustManagers.size}"
        }

        val keyManagers = keyFactory.keyManagers
        check(keyManagers.size == 1 && keyManagers[0] is X509KeyManager) {
            "Expected exactly one key manager, but got ${keyManagers.size}"
        }

        return keyManagers[0] as X509KeyManager
    }

    private fun createEmptyKeyStore(keyStoreType: String? = null): KeyStore {
        val type = keyStoreType ?: KeyStore.getDefaultType()
        return KeyStore.getInstance(type).apply {
            // By convention, 'null' creates an empty key store.
            load(null)
        }
    }

    fun create(
        certificateAuthorities: List<X509Certificate>,
    ): X509TrustManager {
        val keystore = createEmptyKeyStore()

        certificateAuthorities.forEachIndexed { index, certificate ->
            keystore.setCertificateEntry("cert_$index", certificate)
        }

        val managers: MutableList<X509TrustManager> = mutableListOf()
        managers.add(getSystemTrustManager(keystore))
        managers.add(getSystemTrustManager(null))

        return CustomTrustManager(managers)
    }

    private fun getSystemTrustManager(keystore: KeyStore?): X509TrustManager {
        try {
            val trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
            trustManagerFactory.init(keystore)
            for (trustManager in trustManagerFactory.trustManagers) {
                if (trustManager is X509TrustManager) {
                    return trustManager
                }
            }
        } catch (exception: NoSuchAlgorithmException) {
            // Ignore
        } catch (exception: KeyStoreException) {
            // Ignore
        }
        throw IllegalStateException("No X509TrustManager in TrustManagerFactory")
    }
}
