package com.example.budguette

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class PostDetailActivity : AppCompatActivity() {

    private lateinit var titleTextView: TextView
    private lateinit var captionTextView: TextView
    private lateinit var deleteButton: Button
    private lateinit var userImageView: ImageView
    private lateinit var userNameTextView: TextView
    private lateinit var commentEditText: EditText
    private lateinit var postCommentButton: Button
    private lateinit var commentsRecyclerView: RecyclerView

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private lateinit var commentsAdapter: CommentAdapter
    private val commentsList = mutableListOf<Comment>()

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
        commentEditText = findViewById(R.id.commentEditText)
        postCommentButton = findViewById(R.id.postCommentButton)
        commentsRecyclerView = findViewById(R.id.commentsRecyclerView)

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
            showDeleteConfirmationDialog(currentUser.uid)
        }

        userImageView.setOnClickListener {
            openUserProfile(postUserId)
        }

        userNameTextView.setOnClickListener {
            openUserProfile(postUserId)
        }

        fetchUserInfo(postUserId)

        setupCommentsRecyclerView()
        listenForComments()

        postCommentButton.setOnClickListener {
            val commentText = commentEditText.text.toString().trim()
            if (commentText.isNotEmpty()) {
                postComment(commentText)
            }
        }
    }

    private fun openUserProfile(userId: String) {
        val intent = Intent(this, UserProfileActivity::class.java)
        intent.putExtra("userId", userId)
        startActivity(intent)
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

    private fun setupCommentsRecyclerView() {
        commentsAdapter = CommentAdapter(commentsList)

        commentsAdapter.onProfileClicked = { userId ->
            openUserProfile(userId)
        }


        commentsAdapter.onCommentLongClicked = { comment ->
            showDeleteCommentDialog(comment)
        }

        commentsRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@PostDetailActivity)
            adapter = commentsAdapter
        }
    }


    private fun listenForComments() {
        db.collection("posts")
            .document(postId)
            .collection("comments")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshots, error ->
                if (error != null) {
                    Toast.makeText(this, "Failed to load comments", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                commentsList.clear()

                if (snapshots != null) {
                    for (doc in snapshots.documents) {
                        val comment = doc.toObject(Comment::class.java)
                        if (comment != null) {
                            commentsList.add(comment)
                        }
                    }
                    commentsAdapter.notifyDataSetChanged()
                }
            }
    }

    private fun postComment(commentText: String) {
        val currentUser = auth.currentUser ?: return

        db.collection("users").document(currentUser.uid).get()
            .addOnSuccessListener { document ->
                val userName = document.getString("name") ?: "Unknown User"
                val profileImageUrl = document.getString("profileImageUrl") ?: ""

                val comment = Comment(
                    text = commentText,
                    userId = currentUser.uid,
                    userName = userName,
                    profileImageUrl = profileImageUrl,
                    timestamp = System.currentTimeMillis()
                )

                db.collection("posts")
                    .document(postId)
                    .collection("comments")
                    .add(comment)
                    .addOnSuccessListener {
                        commentEditText.text.clear()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Failed to post comment: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to fetch user info: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showDeleteCommentDialog(comment: Comment) {
        val dialogBuilder = AlertDialog.Builder(this)
        dialogBuilder.setMessage("Delete this comment?")
            .setCancelable(true)
            .setPositiveButton("Delete") { dialog, id ->
                deleteComment(comment)
            }
            .setNegativeButton("Cancel") { dialog, id ->
                dialog.dismiss()
            }

        val alert = dialogBuilder.create()
        alert.show()
    }

    private fun deleteComment(comment: Comment) {
        db.collection("posts")
            .document(postId)
            .collection("comments")
            .whereEqualTo("timestamp", comment.timestamp)
            .get()
            .addOnSuccessListener { documents ->
                for (doc in documents) {
                    doc.reference.delete()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to delete comment: ${e.message}", Toast.LENGTH_SHORT).show()
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

    private fun showDeleteConfirmationDialog(currentUid: String) {
        val dialogBuilder = AlertDialog.Builder(this)
        dialogBuilder.setMessage("Are you sure you want to delete your post?")
            .setCancelable(false)
            .setPositiveButton("Yes") { dialog, id ->
                performDelete(currentUid)
            }
            .setNegativeButton("No") { dialog, id ->
                dialog.dismiss()
            }

        val alert = dialogBuilder.create()
        alert.show()
    }
}






