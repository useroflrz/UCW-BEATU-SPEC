package com.ucw.beatu.shared.network.retrofit

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.ucw.beatu.shared.network.config.NetworkConfig
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

object RetrofitProvider {
    fun moshi(): Moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    fun createRetrofit(
        config: NetworkConfig,
        okHttpClient: OkHttpClient,
        moshi: Moshi = moshi()
    ): Retrofit = Retrofit.Builder()
        .baseUrl(config.baseUrl)
        .client(okHttpClient)
        // ✅ 使用 lenient() 模式，允许更宽松的 JSON 解析
        .addConverterFactory(MoshiConverterFactory.create(moshi).asLenient())
        .build()

    inline fun <reified T> createService(retrofit: Retrofit): T = retrofit.create(T::class.java)
}

