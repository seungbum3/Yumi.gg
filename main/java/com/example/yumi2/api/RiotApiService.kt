package com.example.yumi2.api

import retrofit2.Call
import retrofit2.http.GET

data class ChampionRotationResponse(val champion_list: List<ChampionData>)

data class ChampionData(
    val id: Int,
    val name: String,
    val imageUrl: String
)

interface RiotApiService {
    @GET("updateChampionRotation")  // Firebase Functions API 엔드포인트
    fun getChampionRotation(): Call<ChampionRotationResponse>
}
