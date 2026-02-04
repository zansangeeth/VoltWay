package com.sangeeth.voltway.network

import com.sangeeth.voltway.model.ChargingStationResponse
import com.squareup.moshi.Moshi
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException

class MapboxService(private val accessToken: String) {
    private val client = OkHttpClient()
    private val moshi = Moshi.Builder().build()
    private val adapter = moshi.adapter(ChargingStationResponse::class.java)

    @Throws(IOException::class)
    fun fetchChargingStations(latitude: Double, longitude: Double, distanceLimit: Int = 100): ChargingStationResponse? {
        // Using Search Box API instead of restricted EV API
        val url = "https://api.mapbox.com/search/searchbox/v1/category/charging_station?access_token=$accessToken&language=en&limit=25&proximity=$longitude,$latitude"
        android.util.Log.d("MapboxService", "Fetching stations: $url")
        
        val request = Request.Builder()
            .url(url)
            .get()
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                android.util.Log.e("MapboxService", "Response failed: ${response.code} ${response.message}")
                return null
            }
            val body = response.body?.string() ?: return null
            // Log a snippet for debugging if needed
            // android.util.Log.d("MapboxService", "Response body: $body")
            return adapter.fromJson(body)
        }
    }
}
