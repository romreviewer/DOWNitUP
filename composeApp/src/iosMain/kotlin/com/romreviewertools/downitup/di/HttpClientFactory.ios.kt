package com.romreviewertools.downitup.di

import io.ktor.client.*
import io.ktor.client.engine.darwin.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

/**
 * iOS-specific HttpClient factory
 */
actual fun createHttpClient(): HttpClient {
    println("HttpClientFactory.ios: Creating iOS HttpClient with Darwin engine")

    return HttpClient(Darwin) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
            })
        }

        // Enable following redirects
        followRedirects = true

        // Configure Darwin engine
        engine {
            configureRequest {
                setTimeoutInterval(60.0) // 60 seconds
            }
        }
    }
}
