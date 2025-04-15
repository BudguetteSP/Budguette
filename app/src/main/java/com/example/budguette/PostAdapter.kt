package com.example.budguette

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class PostAdapter : RecyclerView.Adapter<PostAdapter.PostViewHolder>() {

    private val postList = mutableListOf<Post>()

    class PostViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val profilePic: ImageView = itemView.findViewById(R.id.profile_image)
        val userName: TextView = itemView.findViewById(R.id.user_name)
        val title: TextView = itemView.findViewById(R.id.post_title)
        val caption: TextView = itemView.findViewById(R.id.post_caption)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_post, parent, false)
        return PostViewHolder(view)
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        val post = postList[position]
        holder.userName.text = post.userName
        holder.title.text = post.title
        holder.caption.text = post.caption

        Glide.with(holder.itemView.context)
            .load(post.profileImageUrl)
            .placeholder(R.drawable.ic_defaultprofile_background)
            .into(holder.profilePic)
    }

    override fun getItemCount(): Int = postList.size

    fun submitList(newList: List<Post>) {
        postList.clear()
        postList.addAll(newList)
        notifyDataSetChanged()
    }
}

