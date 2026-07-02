package rs.tim13.slagalica.dailymissions.data

import android.content.Context
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import rs.tim13.slagalica.core.network.RetrofitClient
import rs.tim13.slagalica.dailymissions.model.DailyMissionsResponse

class DailyMissionsRepositoryImpl(context: Context) : DailyMissionsRepository {

    private val api = RetrofitClient.getClient(context).create(DailyMissionsApiService::class.java)

    override fun get(callback: (DailyMissionsResponse?, String?) -> Unit) {
        api.getDailyMissions().enqueue(object : Callback<DailyMissionsResponse> {
            override fun onResponse(call: Call<DailyMissionsResponse>, response: Response<DailyMissionsResponse>) {
                if (response.isSuccessful) callback(response.body(), null)
                else callback(null, "Greška ${response.code()}")
            }
            override fun onFailure(call: Call<DailyMissionsResponse>, t: Throwable) {
                callback(null, t.message ?: "Greška mreže")
            }
        })
    }
}
