package com.ucw.beatu.core.network.config

import com.ucw.beatu.core.network.interceptor.HeaderInterceptor
import com.ucw.beatu.core.network.interceptor.NetworkLoggingInterceptor
import okhttp3.Cache
import okhttp3.OkHttpClient
import java.io.File
import java.util.concurrent.TimeUnit

object OkHttpProvider {
    fun create(
        config: NetworkConfig,
        cacheDir: File? = null
    ): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .connectTimeout(config.connectTimeoutSeconds, TimeUnit.SECONDS)
            .readTimeout(config.readTimeoutSeconds, TimeUnit.SECONDS)
            .writeTimeout(config.writeTimeoutSeconds, TimeUnit.SECONDS)
            .addInterceptor(HeaderInterceptor(config.defaultHeaders))

        if (config.enableLogging) {
            builder.addInterceptor(NetworkLoggingInterceptor())
        }

        cacheDir?.let {
            builder.cache(Cache(File(it, "okhttp"), 10L * 1024 * 1024))
        }

        return builder.build()
    }
}
