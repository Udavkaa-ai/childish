package ru.sovushka.core.ai

import okhttp3.OkHttpClient
import java.io.ByteArrayInputStream
import java.security.KeyStore
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager

/**
 * Серверы GigaChat подписаны корневым сертификатом НУЦ Минцифры, которого нет в Android.
 * Родитель скачивает его с Госуслуг, а мы доверяем ему только внутри своего HTTP-клиента
 * (вместе с обычными системными сертификатами).
 */
object Tls {
    /** Разобрать сертификаты из PEM или DER. Бросает исключение, если файл не сертификат. */
    fun parseCertificates(bytes: ByteArray): List<X509Certificate> {
        val cf = CertificateFactory.getInstance("X.509")
        val certs = cf.generateCertificates(ByteArrayInputStream(bytes)).filterIsInstance<X509Certificate>()
        require(certs.isNotEmpty()) { "в файле нет сертификатов" }
        return certs
    }

    fun withExtraCertificates(base: OkHttpClient, extra: List<X509Certificate>): OkHttpClient {
        if (extra.isEmpty()) return base
        val system = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm()).apply {
            init(null as KeyStore?)
        }.trustManagers.filterIsInstance<X509TrustManager>().first()

        val ks = KeyStore.getInstance(KeyStore.getDefaultType()).apply { load(null, null) }
        system.acceptedIssuers.forEachIndexed { i, c -> ks.setCertificateEntry("sys$i", c) }
        extra.forEachIndexed { i, c -> ks.setCertificateEntry("extra$i", c) }

        val tm = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm()).apply {
            init(ks)
        }.trustManagers.filterIsInstance<X509TrustManager>().first()
        val ctx = SSLContext.getInstance("TLS").apply { init(null, arrayOf(tm), null) }
        return base.newBuilder().sslSocketFactory(ctx.socketFactory, tm).build()
    }
}
