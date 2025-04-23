package com.example.budguette

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class ForumsFragment : Fragment() {

    private lateinit var postsRecyclerView: RecyclerView
    private lateinit var createPostBtn: FloatingActionButton  // Change to FloatingActionButton
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private lateinit var postAdapter: PostAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_forums, container, false)

        postsRecyclerView = view.findViewById(R.id.posts_recycler_view)
        createPostBtn = view.findViewById(R.id.create_post_button)

        postAdapter = PostAdapter()
        postsRecyclerView.layoutManager = LinearLayoutManager(context)
        postsRecyclerView.adapter = postAdapter

        loadPosts()

        createPostBtn.setOnClickListener {
            // Navigate to CreatePostActivity or CreatePostFragment
            val intent = Intent(requireContext(), CreatePostActivity::class.java)
            startActivity(intent)
        }

        return view
    }

    private fun loadPosts() {
        val adapter = PostAdapter()
        postsRecyclerView.adapter = adapter

        db.collection("posts")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { documents ->
                // Build a list of Posts that includes the document ID
                val postsList = documents.map { doc ->
                    doc.toObject(Post::class.java)
                        .copy(id = doc.id)     // ← copy in the Firestore-generated ID
                }
                adapter.submitList(postsList)
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Failed to load posts: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

}

