package com.example.budguette

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class ForumsFragment : Fragment() {

    private lateinit var postsRecyclerView: RecyclerView
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var createPostBtn: FloatingActionButton
    private lateinit var searchView: SearchView
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private lateinit var postAdapter: PostAdapter
    private var fullPostList: List<Post> = listOf()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_forums, container, false)

        postsRecyclerView = view.findViewById(R.id.posts_recycler_view)
        swipeRefreshLayout = view.findViewById(R.id.swipe_refresh_layout)
        createPostBtn = view.findViewById(R.id.create_post_button)
        searchView = view.findViewById(R.id.search_view)

        postAdapter = PostAdapter()
        postsRecyclerView.layoutManager = LinearLayoutManager(context)
        postsRecyclerView.adapter = postAdapter

        swipeRefreshLayout.setOnRefreshListener {
            loadPosts()
        }

        createPostBtn.setOnClickListener {
            val intent = Intent(requireContext(), CreatePostActivity::class.java)
            startActivity(intent)
        }

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                filterPosts(query)
                searchView.clearFocus()
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                filterPosts(newText)
                return true
            }
        })

        return view
    }

    override fun onResume() {
        super.onResume()
        loadPosts()
    }

    private fun loadPosts() {
        swipeRefreshLayout.isRefreshing = true

        db.collection("posts")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { documents ->
                fullPostList = documents.map { doc ->
                    doc.toObject(Post::class.java).copy(id = doc.id)
                }
                postAdapter.submitList(fullPostList)
                swipeRefreshLayout.isRefreshing = false
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Failed to load posts: ${e.message}", Toast.LENGTH_SHORT).show()
                swipeRefreshLayout.isRefreshing = false
            }
    }

    private fun filterPosts(query: String?) {
        if (query.isNullOrBlank()) {
            postAdapter.submitList(fullPostList)
            return
        }

        val filteredList = fullPostList.filter {
            it.title.contains(query, ignoreCase = true) || it.caption.contains(query, ignoreCase = true)
        }
        postAdapter.submitList(filteredList)
    }
}




