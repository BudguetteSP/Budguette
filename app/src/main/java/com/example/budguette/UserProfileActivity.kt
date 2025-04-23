package com.example.budguette

import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.google.firebase.firestore.FirebaseFirestore

class UserProfileActivity : AppCompatActivity() {

    private lateinit var profileImageView: ImageView
    private lateinit var userNameTextView: TextView
    private lateinit var userBioTextView: TextView

    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_user_profile)

        // Wire up views
        profileImageView = findViewById(R.id.profile_image)
        userNameTextView = findViewById(R.id.profile_user_name)
        userBioTextView = findViewById(R.id.profile_bio)

        // Get user ID from intent
        val userId = intent.getStringExtra("userId") ?: ""

        if (userId.isNotEmpty()) {
            // Fetch and display user data
            fetchUserProfile(userId)
        }
    }

    private fun fetchUserProfile(userId: String) {
        db.collection("users").document(userId).get()
            .addOnSuccessListener { doc ->
                val name = doc.getString("name") ?: "Unknown User"
                val bio = doc.getString("bio") ?: "No bio available."
                Log.d("UserProfileActivity", "Fetched bio: $bio")
                userBioTextView.text = bio ?: "No bio available."
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
}
