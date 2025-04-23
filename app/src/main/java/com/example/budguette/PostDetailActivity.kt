package com.example.budguette

import android.content.Intent
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
    private val db   = FirebaseFirestore.getInstance()

    private var postId: String     = ""
    private var postUserId: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1) Ensure user is authenticated
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(this, "You must be logged in to view this post", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        setContentView(R.layout.activity_post_detail)

        // 2) Wire up views
        titleTextView   = findViewById(R.id.post_detail_title)
        captionTextView = findViewById(R.id.post_detail_caption)
        deleteButton    = findViewById(R.id.delete_post_button)

        // 3) Retrieve Intent extras
        val title   = intent.getStringExtra("title")   ?: ""
        val caption = intent.getStringExtra("caption") ?: ""
        postId      = intent.getStringExtra("postId")     ?: ""
        postUserId  = intent.getStringExtra("postUserId") ?: ""

        // 4) Validate postId
        if (postId.isBlank()) {
            Toast.makeText(this, "Invalid post ID", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // 5) Populate UI
        titleTextView.text   = title
        captionTextView.text = caption

        // 6) Show delete button only if current user is the author
        deleteButton.visibility =
            if (currentUser.uid == postUserId) Button.VISIBLE
            else Button.GONE

        deleteButton.setOnClickListener {
            performDelete(currentUser.uid)
        }
    }

    private fun performDelete(currentUid: String) {
        // Final guard: user must still be authenticated & owner
        if (auth.currentUser?.uid != currentUid || currentUid != postUserId) {
            Toast.makeText(this, "You’re not authorized to delete this post", Toast.LENGTH_SHORT).show()
            return
        }

        db.collection("posts")
            .document(postId)
            .delete()
            .addOnSuccessListener {
                Toast.makeText(this, "Post deleted successfully", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Delete failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }
}




