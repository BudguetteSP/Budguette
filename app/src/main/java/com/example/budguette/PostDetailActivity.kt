package com.example.budguette

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class PostDetailActivity : AppCompatActivity() {

    private lateinit var titleTextView: TextView
    private lateinit var captionTextView: TextView
    private lateinit var deleteButton: Button

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private lateinit var postId: String
    private lateinit var postUserId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_post_detail)

        titleTextView = findViewById(R.id.post_detail_title)
        captionTextView = findViewById(R.id.post_detail_caption)
        deleteButton = findViewById(R.id.delete_post_button)

        val title = intent.getStringExtra("title")
        val caption = intent.getStringExtra("caption")
        postId = intent.getStringExtra("postId") ?: ""
        postUserId = intent.getStringExtra("postUserId") ?: ""

        titleTextView.text = title
        captionTextView.text = caption

        val currentUserId = auth.currentUser?.uid

        if (currentUserId == postUserId) {
            deleteButton.visibility = Button.VISIBLE
        } else {
            deleteButton.visibility = Button.GONE
        }

        deleteButton.setOnClickListener {
            deletePost()
        }
    }

    private fun deletePost() {
        db.collection("posts").document(postId)
            .delete()
            .addOnSuccessListener {
                Toast.makeText(this, "Post deleted.", Toast.LENGTH_SHORT).show()
                finish() // Go back after deleting
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to delete post.", Toast.LENGTH_SHORT).show()
            }
    }
}
