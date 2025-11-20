package com.ucw.beatu.core.network.retrofit

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.ucw.beatu.core.network.config.NetworkConfig
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
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    inline fun <reified T> createService(retrofit: Retrofit): T = retrofit.create(T::class.java)
}
