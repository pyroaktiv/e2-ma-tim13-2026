package rs.tim13.slagalica.core.network

import okhttp3.OkHttp
import android.content.Context
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import rs.tim13.slagalica.BuildConfig
import rs.tim13.slagalica.auth.data.api.AuthApiService
import rs.tim13.slagalica.core.util.TokenManager

object RetrofitClient {
    private val BASE_URL = BuildConfig.API_BASE_URL

    private var retrofit: Retrofit? = null

    fun getClient(context: Context): Retrofit {
        if (retrofit == null) {
            val tokenManager = TokenManager(context)
            val authInterceptor = AuthInterceptor(tokenManager)

            val loggingInterceptor = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }

            val okHttpClient = OkHttpClient.Builder()
                .addInterceptor(authInterceptor)
                .addInterceptor(loggingInterceptor)
                .build()

            retrofit = Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
        }
        return retrofit!!
    }

    fun getAuthClient(context: Context): AuthApiService {
        return getClient(context).create(AuthApiService::class.java)
    }
}