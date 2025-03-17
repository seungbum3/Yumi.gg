package com.example.yumi2.model

data class Item(
        val id: String = "",
        val name: String = "",
        val imageUrl: String = "",
        val tags: List<String> = emptyList(),
        val cost: Int = 0
)

