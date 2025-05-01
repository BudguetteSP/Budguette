package com.example.budguette

data class Subscription(
    val id: String = "",
    val userId: String = "",
    val name: String = "",
    val amount: Double = 0.0,
    val frequency: String = "", // e.g., Monthly, Yearly
    val startDate: String = "", // Format: yyyy-MM-dd
    val notes: String = ""
)
