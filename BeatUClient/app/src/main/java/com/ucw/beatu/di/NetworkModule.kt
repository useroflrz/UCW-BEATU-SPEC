package com.ucw.beatu.di

import android.content.Context
import com.ucw.beatu.R
import com.ucw.beatu.shared.network.config.NetworkConfig
import com.ucw.beatu.shared.network.config.OkHttpProvider
import com.ucw.beatu.shared.network.monitor.ConnectivityObserver
import com.ucw.beatu.shared.network.retrofit.RetrofitProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import javax.inject.Singleton

/**
 * 网络模块
 * 提供Retrofit、OkHttpClient、NetworkConfig等网络相关依赖注入
 * 配置MySQL后端服务连接
 * 
 * 配置说明：
 * - 开发环境配置：app/src/main/res/values/config.xml
 * - 生产环境配置：app/src/main/res/values/config_release.xml（需要在build.gradle.kts中配置buildTypes）
 * - 修改配置时，只需编辑对应的config.xml文件即可
 */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideNetworkConfig(
        @ApplicationContext context: Context
    ): NetworkConfig {
        val baseUrl = context.getString(R.string.base_url)
        val connectTimeout = context.resources.getInteger(R.integer.connect_timeout_seconds).toLong()
        val readTimeout = context.resources.getInteger(R.integer.read_timeout_seconds).toLong()
        val writeTimeout = context.resources.getInteger(R.integer.write_timeout_seconds).toLong()
        val enableLogging = context.resources.getBoolean(R.bool.enable_network_logging)
        
        return NetworkConfig(
            baseUrl = baseUrl,
            connectTimeoutSeconds = connectTimeout,
            readTimeoutSeconds = readTimeout,
            writeTimeoutSeconds = writeTimeout,
            enableLogging = enableLogging,
            defaultHeaders = mapOf(
                "Content-Type" to "application/json",
                "Accept" to "application/json"
                // TODO: 如果需要token认证，可以在这里添加 Authorization header
            )
        )
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(
        config: NetworkConfig,
        @ApplicationContext context: Context
    ): OkHttpClient {
        return OkHttpProvider.create(config, context.cacheDir)
    }

    @Provides
    @Singleton
    fun provideRetrofit(
        config: NetworkConfig,
        okHttpClient: OkHttpClient
    ): Retrofit {
        return RetrofitProvider.createRetrofit(config, okHttpClient)
    }

    @Provides
    @Singleton
    fun provideConnectivityObserver(
        @ApplicationContext context: Context
    ): ConnectivityObserver {
        return ConnectivityObserver(context)
    }
}

