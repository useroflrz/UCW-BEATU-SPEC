package com.ucw.beatu.core.network.config

data class NetworkConfig(
    val baseUrl: String,
    val connectTimeoutSeconds: Long = 15,
    val readTimeoutSeconds: Long = 15,
    val writeTimeoutSeconds: Long = 15,
    val enableLogging: Boolean = true,
    val defaultHeaders: Map<String, String> = emptyMap()
)
