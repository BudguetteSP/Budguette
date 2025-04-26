package com.example.budguette

import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.firestore.FirebaseFirestore

class UserProfileActivity : AppCompatActivity() {

    private lateinit var profileImageView: ImageView
    private lateinit var userNameTextView: TextView
    private lateinit var userBioTextView: TextView
    private lateinit var userPostsRecyclerView: RecyclerView
    private lateinit var postAdapter: PostAdapter

    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_profile)

        // Bind views
        profileImageView = findViewById(R.id.profile_image)
        userNameTextView = findViewById(R.id.profile_user_name)
        userBioTextView = findViewById(R.id.profile_bio)
        userPostsRecyclerView = findViewById(R.id.user_posts_recycler_view)

        // RecyclerView setup
        userPostsRecyclerView.layoutManager = LinearLayoutManager(this)
        postAdapter = PostAdapter()
        userPostsRecyclerView.adapter = postAdapter

        // Get user ID from intent
        val userId = intent.getStringExtra("userId") ?: ""

        if (userId.isNotEmpty()) {
            fetchUserProfile(userId)
            fetchUserPosts(userId)
        }
    }

    private fun fetchUserProfile(userId: String) {
        db.collection("users").document(userId).get()
            .addOnSuccessListener { doc ->
                val name = doc.getString("name") ?: "Unknown User"
                val bio = doc.getString("bio") ?: "No bio available."
                val profileImageUrl = doc.getString("profileImageUrl")

                userNameTextView.text = name
                userBioTextView.text = bio

                if (!profileImageUrl.isNullOrEmpty()) {
                    Glide.with(this)
                        .load(profileImageUrl)
                        .placeholder(R.drawable.ic_defaultprofile_background)
                        .circleCrop()
                        .into(profileImageView)
                } else {
                    profileImageView.setImageResource(R.drawable.ic_defaultprofile_background)
                }
            }
            .addOnFailureListener {
                userNameTextView.text = "User"
                userBioTextView.text = "No bio available."
                profileImageView.setImageResource(R.drawable.ic_defaultprofile_background)
            }
    }

    private fun fetchUserPosts(userId: String) {
        db.collection("posts")
            .whereEqualTo("userId", userId)
            .get()
            .addOnSuccessListener { querySnapshot ->
                val posts = querySnapshot.documents.map { doc ->
                    Post(
                        userId = doc.getString("userId") ?: "",
                        userName = doc.getString("userName") ?: "",
                        profileImageUrl = doc.getString("profileImageUrl") ?: "",
                        title = doc.getString("title") ?: "",
                        caption = doc.getString("caption") ?: "",
                        timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis(),
                        id = doc.id
                    )
                }
                postAdapter.submitList(posts)
            }
            .addOnFailureListener { e ->
                Log.e("UserProfileActivity", "Failed to load posts: ${e.message}")
            }
    }
}

