package com.example.budguette

import java.io.Serializable

data class Subscription(
    val id: String = "",
    val userId: String = "",
    val name: String = "",
    val amount: Double = 0.0,
    val frequency: String = "",
    val startDate: String = "",
    val notes: String = ""
) : Serializable

