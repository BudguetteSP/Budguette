package com.example.budguette

import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class SignUpActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_up)

        val btnSubmit = findViewById<Button>(R.id.btnSubmitSignUp)
        btnSubmit.setOnClickListener {
            Toast.makeText(this, "Account Created (placeholder)", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
}