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
    private lateinit var transactionAdapter: TransactionAdapter
    private val transactions = mutableListOf<Transaction>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_expenses, container, false)

        recyclerView = view.findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        transactionAdapter = TransactionAdapter(transactions)
        recyclerView.adapter = transactionAdapter

        loadTransactions()

        val addButton: Button = view.findViewById(R.id.addButton)
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
}


