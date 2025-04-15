package com.example.budguette

data class Post(
    val userId: String = "",
    val userName: String = "",
    val profileImageUrl: String = "",
    val title: String = "",
    val caption: String = "",
    val timestamp: Long = System.currentTimeMillis() // Timestamp for sorting posts
)
