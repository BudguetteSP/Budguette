package com.example.budguette

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class PostDetailActivity : AppCompatActivity() {

    private lateinit var titleTextView: TextView
    private lateinit var captionTextView: TextView
    private lateinit var deleteButton: Button
    private lateinit var userImageView: ImageView
    private lateinit var userNameTextView: TextView

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private var postId: String = ""
    private var postUserId: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(this, "You must be logged in to view this post", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        setContentView(R.layout.activity_post_detail)

        // Wire views
        titleTextView = findViewById(R.id.post_detail_title)
        captionTextView = findViewById(R.id.post_detail_caption)
        deleteButton = findViewById(R.id.delete_post_button)
        userImageView = findViewById(R.id.post_user_image)
        userNameTextView = findViewById(R.id.post_user_name)

        // Get extras
        val title = intent.getStringExtra("title") ?: ""
        val caption = intent.getStringExtra("caption") ?: ""
        postId = intent.getStringExtra("postId") ?: ""
        postUserId = intent.getStringExtra("postUserId") ?: ""

        if (postId.isBlank()) {
            Toast.makeText(this, "Invalid post ID", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        titleTextView.text = title
        captionTextView.text = caption

        deleteButton.visibility =
            if (currentUser.uid == postUserId) Button.VISIBLE else Button.GONE

        deleteButton.setOnClickListener {
            performDelete(currentUser.uid)
        }

        // Fetch and display user info
        fetchUserInfo(postUserId)
    }

    private fun fetchUserInfo(userId: String) {
        db.collection("users").document(userId).get()
            .addOnSuccessListener { doc ->
                val name = doc.getString("name") ?: "Unknown User"
                val profileUrl = doc.getString("profileImageUrl")

                userNameTextView.text = name

                if (!profileUrl.isNullOrEmpty()) {
                    Glide.with(this)
                        .load(profileUrl)
                        .placeholder(R.drawable.ic_defaultprofile_background)
                        .circleCrop()
                        .into(userImageView)
                } else {
                    userImageView.setImageResource(R.drawable.ic_defaultprofile_background)
                }
            }
            .addOnFailureListener {
                userNameTextView.text = "User"
                userImageView.setImageResource(R.drawable.ic_defaultprofile_background)
            }
    }

    private fun performDelete(currentUid: String) {
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



