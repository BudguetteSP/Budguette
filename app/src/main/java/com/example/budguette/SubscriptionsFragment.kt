package com.example.budguette

import android.content.Intent
import android.os.Bundle
import android.view.*
import android.widget.*
import android.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import java.text.SimpleDateFormat
import java.util.*

class SubscriptionsFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: SubscriptionAdapter

    private lateinit var frequencySpinner: Spinner
    private lateinit var amountSpinner: Spinner
    private lateinit var dueSpinner: Spinner

    private val auth by lazy { FirebaseAuth.getInstance() }
    private val db by lazy { FirebaseFirestore.getInstance() }
    private var listener: ListenerRegistration? = null

    private var originalSubscriptions: List<Subscription> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_subscriptions, container, false)

        recyclerView = view.findViewById(R.id.subscriptionRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        adapter = SubscriptionAdapter { subscription, newDate ->
            updateSubscriptionDate(subscription, newDate)
        }

        recyclerView.adapter = adapter

        view.findViewById<FloatingActionButton>(R.id.addSubscriptionFab).setOnClickListener {
            startActivity(Intent(requireContext(), AddSubscriptionActivity::class.java))
        }

        // SearchView
        val searchView = view.findViewById<SearchView>(R.id.subscription_search_view)
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean = false
            override fun onQueryTextChange(newText: String?): Boolean {
                applyFilters(searchQuery = newText ?: "")
                return true
            }
        })

        // Spinners
        frequencySpinner = view.findViewById(R.id.frequencySpinner)
        amountSpinner = view.findViewById(R.id.amountSpinner)
        dueSpinner = view.findViewById(R.id.dueSpinner)

        setupSpinners()

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

                originalSubscriptions = snapshot?.documents?.mapNotNull {
                    it.toObject(Subscription::class.java)
                } ?: emptyList()

                applyFilters()
            }
    }

    override fun onStop() {
        super.onStop()
        listener?.remove()
    }

    private fun setupSpinners() {
        // Frequency Spinner
        val freqOptions = listOf("All", "One-Time", "Daily", "Weekly", "Monthly", "Yearly")
        frequencySpinner.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, freqOptions).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }

        // Amount Spinner
        val amountOptions = listOf("All", "Under $10", "$10 - $50", "$50 - $100", "Over $100")
        amountSpinner.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, amountOptions).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }

        // Due Spinner
        val dueOptions = listOf("All", "Due This Week", "Due This Month", "Due Next Month")
        dueSpinner.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, dueOptions).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }

        val listener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                applyFilters()
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        frequencySpinner.onItemSelectedListener = listener
        amountSpinner.onItemSelectedListener = listener
        dueSpinner.onItemSelectedListener = listener
    }

    private fun applyFilters(searchQuery: String = "") {
        var filtered = originalSubscriptions

        // Search filter
        if (searchQuery.isNotBlank()) {
            filtered = filtered.filter { it.name.contains(searchQuery, ignoreCase = true) }
        }

        // Frequency filter
        val freqSelected = frequencySpinner.selectedItem as String
        if (freqSelected != "All") {
            filtered = filtered.filter { it.frequency == freqSelected }
        }

        // Amount filter
        val amountSelected = amountSpinner.selectedItem as String
        filtered = filtered.filter {
            when (amountSelected) {
                "Under $10" -> it.amount < 10
                "$10 - $50" -> it.amount in 10.0..50.0
                "$50 - $100" -> it.amount in 50.0..100.0
                "Over $100" -> it.amount > 100
                else -> true
            }
        }

        // Due filter
        val dueSelected = dueSpinner.selectedItem as String
        val today = Calendar.getInstance()
        filtered = filtered.filter { sub ->
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val dueDate = sdf.parse(sub.startDate) ?: return@filter true
            when (dueSelected) {
                "Due This Week" -> {
                    val week = today.get(Calendar.WEEK_OF_YEAR)
                    val year = today.get(Calendar.YEAR)
                    val cal = Calendar.getInstance().apply { time = dueDate }
                    cal.get(Calendar.WEEK_OF_YEAR) == week && cal.get(Calendar.YEAR) == year
                }
                "Due This Month" -> {
                    today.get(Calendar.MONTH) == Calendar.getInstance().apply { time = dueDate }.get(Calendar.MONTH)
                }
                "Due Next Month" -> {
                    val cal = Calendar.getInstance().apply { time = dueDate }
                    cal.get(Calendar.MONTH) == today.get(Calendar.MONTH) + 1
                }
                else -> true
            }
        }

        adapter.setSubscriptions(filtered)
    }

    private fun updateSubscriptionDate(subscription: Subscription, newDate: String) {
        db.collection("subscriptions")
            .document(subscription.id)
            .update("startDate", newDate)
            .addOnSuccessListener { Toast.makeText(context, "Start date updated", Toast.LENGTH_SHORT).show() }
            .addOnFailureListener { Toast.makeText(context, "Update failed", Toast.LENGTH_SHORT).show() }
    }
}


