package rs.tim13.slagalica.core.network

import okhttp3.OkHttp
import android.content.Context
import com.google.gson.FieldNamingPolicy
import com.google.gson.GsonBuilder
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

            val gson = GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).create()

            retrofit = Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build()
        }
        return retrofit!!
    }

    fun getAuthClient(context: Context): AuthApiService {
        return getClient(context).create(AuthApiService::class.java)
    }
}