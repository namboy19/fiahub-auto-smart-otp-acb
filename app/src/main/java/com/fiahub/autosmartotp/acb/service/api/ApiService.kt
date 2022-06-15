package com.fiahub.autosmartotp.acb.service.api

import com.fiahub.autosmartotp.acb.BuildConfig
import com.jakewharton.retrofit2.adapter.kotlin.coroutines.CoroutineCallAdapterFactory
import kotlinx.coroutines.Deferred
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query
import java.net.CookieHandler
import java.net.CookieManager
import java.net.CookiePolicy
import java.util.concurrent.TimeUnit

interface ApiService {

    companion object {
        val apiService by lazy {

            Retrofit.Builder()
                .baseUrl("https://www.coingiatot.com/")
                .addConverterFactory(GsonConverterFactory.create())
                .addCallAdapterFactory(CoroutineCallAdapterFactory())
                .client(provideOkHttpClient())
                .build().create(ApiService::class.java)
        }
    }

    @POST("https://www.coingiatot.com/api/v1/bank_otps?secret=0c87be9905a7455aa23896d1aabbbf2f&bank=ACB")
    fun sendOtp(@Query("otp") otp: String): Deferred<Any>

}

private fun provideOkHttpClient(): OkHttpClient {

    val cookieManager = CookieManager()
    CookieHandler.setDefault(cookieManager)
    cookieManager.setCookiePolicy(CookiePolicy.ACCEPT_ALL)

    val httpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)

    if (BuildConfig.DEBUG) {
        httpClient.addInterceptor(
            HttpLoggingInterceptor()
                .setLevel(HttpLoggingInterceptor.Level.BODY))
    }

    return httpClient.build()
}


