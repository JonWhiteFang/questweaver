package dev.questweaver.ai.gateway.di

import dev.questweaver.ai.gateway.AIGatewayApi
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import org.koin.dsl.module

val aiGatewayModule = module {
    single {
        OkHttpClient.Builder()
            .addInterceptor(HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY))
            .build()
    }
    single {
        Retrofit.Builder()
            .baseUrl("https://example.com/") // TODO set via BuildConfig/flavor
            .addConverterFactory(Json { ignoreUnknownKeys = true }.asConverterFactory("application/json".toMediaType()))
            .client(get())
            .build()
            .create(AIGatewayApi::class.java)
    }
}
