package com.romreviewertools.downitup.di

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

/**
 * JVM-specific HttpClient factory
 */
actual fun createHttpClient(): HttpClient {
    println("HttpClientFactory.jvm: Creating JVM HttpClient with CIO engine")

    return HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
            })
        }

        // Enable following redirects
        followRedirects = true

        // Configure CIO engine
        engine {
            requestTimeout = 60_000 // 60 seconds
        }
    }
}
