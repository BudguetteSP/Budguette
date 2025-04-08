package com.example.budguette

import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class ForgotPasswordActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_forgot_password)

        val btnSubmit = findViewById<Button>(R.id.btnSubmitForgotPassword)
        btnSubmit.setOnClickListener {
            Toast.makeText(this, "Reset link sent to your email (placeholder)", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
}
