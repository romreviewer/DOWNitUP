package com.romreviewertools.downitup.di

import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager
import java.util.concurrent.TimeUnit

/**
 * Android-specific HttpClient factory with proper SSL configuration
 */
actual fun createHttpClient(): HttpClient {
    println("HttpClientFactory.android: Creating Android HttpClient with OkHttp engine")

    return HttpClient(OkHttp) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
            })
        }

        // Enable following redirects
        followRedirects = true

        // Configure OkHttp engine
        engine {
            config {
                // Connection timeout
                connectTimeout(30, TimeUnit.SECONDS)

                // Read timeout for large downloads
                readTimeout(60, TimeUnit.SECONDS)

                // Write timeout
                writeTimeout(60, TimeUnit.SECONDS)

                // Follow redirects
                followRedirects(true)
                followSslRedirects(true)

                // Retry on connection failure
                retryOnConnectionFailure(true)

                println("HttpClientFactory.android: OkHttp engine configured")
            }
        }
    }
}
