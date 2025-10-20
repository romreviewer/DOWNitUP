package com.romreviewertools.downitup.di

import io.ktor.client.*

/**
 * Platform-specific HttpClient factory
 * Each platform can configure its own HTTP engine with specific SSL/TLS settings
 */
expect fun createHttpClient(): HttpClient
