package com.ucw.beatu.shared.network.config

data class NetworkConfig(
    val baseUrl: String,
    val connectTimeoutSeconds: Long = 15,
    val readTimeoutSeconds: Long = 15,
    val writeTimeoutSeconds: Long = 15,
    val enableLogging: Boolean = true,
    val defaultHeaders: Map<String, String> = emptyMap(),
    val cacheSizeBytes: Long = 10L * 1024 * 1024 // 默认 10MB
)

