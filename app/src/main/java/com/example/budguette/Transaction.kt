// Transaction.kt
package com.example.budguette

data class Transaction(
    val id: String = "",
    val type: String = "",
    val name: String = "",
    val date: Long = 0L,
    val cost: Double = 0.0,
    val notes: String = "",
    val category: String = ""
)


