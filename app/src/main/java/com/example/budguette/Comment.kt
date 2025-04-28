package com.example.budguette

data class Comment(
    val userId: String = "",
    val userName: String = "",
    val profileImageUrl: String = "",
    val text: String = "",
    val timestamp: Long = System.currentTimeMillis()
)
