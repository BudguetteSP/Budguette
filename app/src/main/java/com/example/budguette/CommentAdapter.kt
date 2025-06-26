package com.example.budguette

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth

class CommentAdapter(private val comments: List<Comment>) :
    RecyclerView.Adapter<CommentAdapter.CommentViewHolder>() {

    var onCommentLongClicked: ((Comment) -> Unit)? = null
    var onProfileClicked: ((String) -> Unit)? = null // New: Handle profile clicks

    inner class CommentViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val profileImage: ImageView = view.findViewById(R.id.comment_user_image)
        val userName: TextView = view.findViewById(R.id.comment_user_name)
        val commentText: TextView = view.findViewById(R.id.comment_text)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommentViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_comment, parent, false)
        return CommentViewHolder(view)
    }

    override fun onBindViewHolder(holder: CommentViewHolder, position: Int) {
        val comment = comments[position]
        holder.userName.text = comment.userName
        holder.commentText.text = comment.text

        Glide.with(holder.profileImage.context)
            .load(comment.profileImageUrl)
            .placeholder(R.drawable.ic_defaultprofile_background)
            .circleCrop()
            .into(holder.profileImage)

        val auth = FirebaseAuth.getInstance()
        val currentUser = auth.currentUser

        // Allow delete only for own comments
        if (currentUser != null && comment.userId == currentUser.uid) {
            holder.itemView.setOnLongClickListener {
                onCommentLongClicked?.invoke(comment)
                true
            }
        }

        // New: Set click listeners for profile image and name
        holder.profileImage.setOnClickListener {
            onProfileClicked?.invoke(comment.userId)
        }

        holder.userName.setOnClickListener {
            onProfileClicked?.invoke(comment.userId)
        }
    }

    override fun getItemCount() = comments.size
}
