package com.example.budguette

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class CreatePostActivity : AppCompatActivity() {

    private lateinit var titleEditText: EditText
    private lateinit var captionEditText: EditText
    private lateinit var createPostBtn: Button
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_post)

        titleEditText = findViewById(R.id.post_title)
        captionEditText = findViewById(R.id.post_caption)
        createPostBtn = findViewById(R.id.submit_post_btn)

        createPostBtn.setOnClickListener {
            val title = titleEditText.text.toString()
            val caption = captionEditText.text.toString()
            val userId = FirebaseAuth.getInstance().currentUser?.uid

            if (title.isNotEmpty() && caption.isNotEmpty() && userId != null) {
                val post = Post(
                    userId = userId,
                    title = title,
                    caption = caption,
                    timestamp = System.currentTimeMillis()
                )

                // Save post to Firestore
                db.collection("posts")
                    .add(post)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Post created successfully!", Toast.LENGTH_SHORT).show()
                        finish() // Go back to ForumsFragment
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Failed to create post: ${e.message}", Toast.LENGTH_LONG).show()
                        e.printStackTrace()
                    }
            } else {
                Toast.makeText(this, "Please fill out both fields.", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
