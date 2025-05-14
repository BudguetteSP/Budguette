package com.example.budguette

import android.content.Intent
import android.os.Bundle
import android.view.*
import android.widget.Toast
import android.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query

class SubscriptionsFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: SubscriptionAdapter
    private val auth by lazy { FirebaseAuth.getInstance() }
    private val db by lazy { FirebaseFirestore.getInstance() }
    private var listener: ListenerRegistration? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_subscriptions, container, false)

        // Set up RecyclerView
        recyclerView = view.findViewById(R.id.subscriptionRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        adapter = SubscriptionAdapter { subscription, newDate ->
            updateSubscriptionDate(subscription, newDate)
        }

        recyclerView.adapter = adapter

        // Set up FAB to add new subscription
        view.findViewById<FloatingActionButton>(R.id.addSubscriptionFab).setOnClickListener {
            startActivity(Intent(requireContext(), AddSubscriptionActivity::class.java))
        }

        // Set up SearchView
        val searchView = view.findViewById<SearchView>(R.id.subscription_search_view)
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean = false
            override fun onQueryTextChange(newText: String?): Boolean {
                adapter.filter.filter(newText)
                return true
            }
        })

        return view
    }

    override fun onStart() {
        super.onStart()
        val userId = auth.currentUser?.uid ?: return

        listener = db.collection("subscriptions")
            .whereEqualTo("userId", userId)
            .orderBy("startDate", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener

                val updatedList = snapshot?.documents?.mapNotNull {
                    it.toObject(Subscription::class.java)
                } ?: emptyList()

                adapter.setSubscriptions(updatedList)
            }
    }

    override fun onStop() {
        super.onStop()
        listener?.remove()
    }

    private fun updateSubscriptionDate(subscription: Subscription, newDate: String) {
        db.collection("subscriptions")
            .document(subscription.id)
            .update("startDate", newDate)
            .addOnSuccessListener {
                Toast.makeText(context, "Start date updated", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(context, "Update failed", Toast.LENGTH_SHORT).show()
            }
    }
}
