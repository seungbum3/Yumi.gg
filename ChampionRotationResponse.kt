package com.example.yumi

data class ChampionRotationResponse(
    val freeChampionIds: List<Int>,
    val freeChampionIdsForNewPlayers: List<Int>,
    val maxNewPlayerLevel: Int
)
