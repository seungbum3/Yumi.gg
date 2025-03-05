package com.example.yumi2

import com.google.firebase.Timestamp

data class ChatMessage(
    val senderId: String = "",
    val message: String = "",
    val timestamp: Timestamp? = null
)
