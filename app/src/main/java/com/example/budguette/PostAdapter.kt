package com.example.budguette

import android.content.Intent
import android.graphics.Color
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage

class PostAdapter : RecyclerView.Adapter<PostAdapter.PostViewHolder>() {

    private val postList = mutableListOf<Post>()
    private var searchQuery: String? = null

    fun submitList(newList: List<Post>, query: String? = null) {
        postList.clear()
        postList.addAll(newList)
        searchQuery = query
        notifyDataSetChanged()
    }

    inner class PostViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val profilePic: ImageView = itemView.findViewById(R.id.profile_image)
        val userName: TextView = itemView.findViewById(R.id.user_name)
        val title: TextView = itemView.findViewById(R.id.post_title)
        val caption: TextView = itemView.findViewById(R.id.post_caption)

        init {
            itemView.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val post = postList[position]
                    val context = itemView.context
                    val intent = Intent(context, PostDetailActivity::class.java).apply {
                        putExtra("postId", post.id)
                        putExtra("title", post.title)
                        putExtra("caption", post.caption)
                        putExtra("postUserId", post.userId)
                    }
                    context.startActivity(intent)
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_post, parent, false)
        return PostViewHolder(view)
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        val post = postList[position]
        holder.title.text = getHighlightedText(post.title, searchQuery)
        holder.caption.text = getHighlightedText(post.caption, searchQuery)

        val storageRef = FirebaseStorage.getInstance().reference
        val profilePicRef = storageRef.child("profile_pictures/${post.userId}.jpg")

        profilePicRef.downloadUrl
            .addOnSuccessListener { uri ->
                Glide.with(holder.itemView.context)
                    .load(uri)
                    .placeholder(R.drawable.ic_defaultprofile_background)
                    .circleCrop()
                    .into(holder.profilePic)
            }
            .addOnFailureListener {
                holder.profilePic.setImageResource(R.drawable.ic_defaultprofile_background)
            }

        val db = FirebaseFirestore.getInstance()
        db.collection("users").document(post.userId).get()
            .addOnSuccessListener { doc ->
                val name = doc.getString("name") ?: "Unknown"
                holder.userName.text = name
            }
            .addOnFailureListener {
                holder.userName.text = "Unknown"
            }
    }

    override fun getItemCount(): Int = postList.size

    private fun getHighlightedText(text: String, query: String?): CharSequence {
        if (query.isNullOrBlank()) return text

        val spannable = SpannableString(text)
        val lowerText = text.lowercase()
        val lowerQuery = query.lowercase()

        var startIndex = lowerText.indexOf(lowerQuery)
        while (startIndex >= 0) {
            val endIndex = startIndex + query.length

            // Bold style
            spannable.setSpan(
                android.text.style.StyleSpan(android.graphics.Typeface.BOLD),
                startIndex, endIndex,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )

            // Subtle background highlight (light gray)
            spannable.setSpan(
                android.text.style.BackgroundColorSpan(Color.parseColor("#D3D3D3")), // light gray
                startIndex, endIndex,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )

            startIndex = lowerText.indexOf(lowerQuery, endIndex)
        }

        return spannable
    }
}


