// ExpensesFragment.kt
package com.example.budguette

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ExpensesFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: TransactionAdapter
    private lateinit var transactionList: ArrayList<Transaction>

    private lateinit var addButton: Button
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_expenses, container, false)

        recyclerView = view.findViewById(R.id.recyclerView)
        addButton = view.findViewById(R.id.addButton)

        transactionList = ArrayList()
        adapter = TransactionAdapter(transactionList)

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter

        addButton.setOnClickListener {
            startActivity(Intent(requireContext(), AddTransactionActivity::class.java))
        }

        fetchTransactions()

        return view
    }

    private fun fetchTransactions() {
        val userId = auth.currentUser?.uid ?: return

        db.collection("users").document(userId).collection("transactions")
            .orderBy("date")
            .addSnapshotListener { snapshot, e ->
                if (e != null || snapshot == null) {
                    return@addSnapshotListener
                }

                transactionList.clear()
                for (doc in snapshot.documents) {
                    val transaction = doc.toObject(Transaction::class.java)
                    transaction?.let { transactionList.add(it) }
                }
                adapter.notifyDataSetChanged()
            }
    }
}

