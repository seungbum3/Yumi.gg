package com.example.yumi

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface RiotApiService {
    @GET("lol/platform/v3/champion-rotations")
    fun getChampionRotation(@Query("api_key") apiKey: String): Call<ChampionRotationResponse>
}
