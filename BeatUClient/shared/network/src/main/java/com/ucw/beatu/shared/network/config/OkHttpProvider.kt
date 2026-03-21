package com.ucw.beatu.shared.network.config

import com.ucw.beatu.shared.network.interceptor.HeaderInterceptor
import com.ucw.beatu.shared.network.interceptor.NetworkLoggingInterceptor
import okhttp3.Cache
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import java.io.File
import java.util.concurrent.TimeUnit

object OkHttpProvider {
    fun create(
        config: NetworkConfig,
        cacheDir: File? = null,
        extraInterceptors: List<Interceptor> = emptyList()
    ): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .connectTimeout(config.connectTimeoutSeconds, TimeUnit.SECONDS)
            .readTimeout(config.readTimeoutSeconds, TimeUnit.SECONDS)
            .writeTimeout(config.writeTimeoutSeconds, TimeUnit.SECONDS)
            .addInterceptor(HeaderInterceptor(config.defaultHeaders))
        extraInterceptors.forEach { builder.addInterceptor(it) }

        if (config.enableLogging) {
            builder.addInterceptor(NetworkLoggingInterceptor())
        }

        cacheDir?.let {
            builder.cache(Cache(File(it, "okhttp"), config.cacheSizeBytes))
        }

        return builder.build()
    }
}

