package com.example.budguette

import android.content.Intent
import android.os.Bundle
import android.view.*
import android.widget.Toast
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
    private val subscriptions = mutableListOf<Subscription>()

    private val auth by lazy { FirebaseAuth.getInstance() }
    private val db   by lazy { FirebaseFirestore.getInstance() }

    private var listener: ListenerRegistration? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_subscriptions, container, false)

        // RecyclerView & adapter
        recyclerView = view.findViewById(R.id.subscriptionRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        adapter = SubscriptionAdapter(subscriptions) { subscription, newDate ->
            updateSubscriptionDate(subscription, newDate)
        }
        recyclerView.adapter = adapter

        // FAB to add new subscription
        view.findViewById<FloatingActionButton>(R.id.addSubscriptionFab)
            .setOnClickListener {
                startActivity(Intent(requireContext(), AddSubscriptionActivity::class.java))
            }

        return view
    }

    override fun onStart() {
        super.onStart()
        // Attach real-time listener
        val userId = auth.currentUser?.uid ?: return
        listener = db.collection("subscriptions")
            .whereEqualTo("userId", userId)
            .orderBy("startDate", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Toast.makeText(context, "Failed to load subscriptions", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }
                subscriptions.clear()
                snapshot?.documents?.forEach { doc ->
                    doc.toObject(Subscription::class.java)?.let { subscriptions.add(it) }
                }
                adapter.notifyDataSetChanged()
            }
    }

    override fun onStop() {
        super.onStop()
        // Detach listener to avoid leaks
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



