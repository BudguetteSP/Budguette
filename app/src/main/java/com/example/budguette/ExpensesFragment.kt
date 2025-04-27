package com.example.budguette

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ExpensesFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var transactionAdapter: TransactionAdapter
    private val transactions = mutableListOf<Transaction>()
    private lateinit var searchView: SearchView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_expenses, container, false)

        recyclerView = view.findViewById(R.id.recyclerViewTransactions)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        transactionAdapter = TransactionAdapter(transactions)
        recyclerView.adapter = transactionAdapter
        loadTransactions()

        searchView = view.findViewById(R.id.searchView) as SearchView


        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                filterTransactions(newText)
                return true
            }
        })

        val addButton: Button = view.findViewById(R.id.addTransactionButton)
        addButton.setOnClickListener {
            startActivity(Intent(requireContext(), AddTransactionActivity::class.java))
        }

        return view
    }

    private fun loadTransactions() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val db = FirebaseFirestore.getInstance()

        db.collection("users")
            .document(userId)
            .collection("transactions")
            .orderBy("date", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { querySnapshot ->
                transactions.clear()
                for (doc in querySnapshot) {
                    val transaction = doc.toObject(Transaction::class.java)
                    transactions.add(transaction)
                }
                transactionAdapter.notifyDataSetChanged()
            }
            .addOnFailureListener { e ->
                e.printStackTrace()
            }
    }

    private fun filterTransactions(query: String?) {
        if (query.isNullOrEmpty()) {
            transactionAdapter.updateList(transactions)
        } else {
            val filteredList = transactions.filter {
                it.name.contains(query, ignoreCase = true) ||
                        it.type.contains(query, ignoreCase = true)
            }
            transactionAdapter.updateList(filteredList)
        }
    }

}


