package com.example.budguette

data class MonthlyReport(
    val month: String = "",
    val spent: Double = 0.0,
    val overBudget: Boolean = false
)
