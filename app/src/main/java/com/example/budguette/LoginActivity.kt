package com.example.budguette

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class LoginActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val loginButton = findViewById<Button>(R.id.btnLogin)
        loginButton.setOnClickListener {
            // (Here you'd normally validate credentials)
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish() // prevent going back to login
        }
    }
}