package com.example.budguette

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.Button
import android.widget.Spinner
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.slider.RangeSlider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Calendar

class ExpensesFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var transactionAdapter: TransactionAdapter
    private val transactions = mutableListOf<Transaction>()
    private lateinit var searchView: SearchView
    private var currentQuery: String? = null

    // 🔹 New filter UI elements
    private lateinit var spinnerType: Spinner
    private lateinit var spinnerCategory: Spinner
    private lateinit var priceSlider: RangeSlider
    private lateinit var buttonDateFilter: Button
    private var selectedDate: Long? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_expenses, container, false)

        // RecyclerView setup
        recyclerView = view.findViewById(R.id.recyclerViewTransactions)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        transactionAdapter = TransactionAdapter(transactions)
        recyclerView.adapter = transactionAdapter
        loadTransactions()

        // Search setup
        searchView = view.findViewById(R.id.searchView)
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean = false
            override fun onQueryTextChange(newText: String?): Boolean {
                filterTransactions(newText)
                return true
            }
        })

        // FAB setup
        val addButton: FloatingActionButton = view.findViewById(R.id.addTransactionButton)
        addButton.setOnClickListener {
            startActivity(Intent(requireContext(), AddTransactionActivity::class.java))
        }

        // 🔹 Filter UI setup
        spinnerType = view.findViewById(R.id.spinnerType)
        spinnerCategory = view.findViewById(R.id.spinnerCategory)
        priceSlider = view.findViewById(R.id.sliderPrice)
        buttonDateFilter = view.findViewById(R.id.buttonDateFilter)

        spinnerType.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, v: View?, pos: Int, id: Long) {
                filterTransactions(currentQuery)
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        spinnerCategory.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, v: View?, pos: Int, id: Long) {
                filterTransactions(currentQuery)
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        priceSlider.addOnChangeListener { _, _, _ ->
            filterTransactions(currentQuery)
        }

        buttonDateFilter.setOnClickListener {
            val cal = Calendar.getInstance()
            val picker = DatePickerDialog(
                requireContext(),
                { _, year, month, day ->
                    val pickedCal = Calendar.getInstance()
                    pickedCal.set(year, month, day, 0, 0, 0)
                    selectedDate = pickedCal.timeInMillis
                    filterTransactions(currentQuery)
                },
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)
            )
            picker.show()
        }

        // Handle clicks on transactions
        transactionAdapter.setOnItemClickListener { transaction ->
            val intent = Intent(requireContext(), TransactionDetailActivity::class.java).apply {
                putExtra("transactionId", transaction.id)
                putExtra("userId", FirebaseAuth.getInstance().currentUser?.uid)
                putExtra("name", transaction.name)
                putExtra("type", transaction.type)
                putExtra(
                    "date",
                    android.text.format.DateFormat.format("MMM dd, yyyy", transaction.date)
                        .toString()
                )
                putExtra("cost", transaction.cost)
                putExtra("notes", transaction.notes)
                putExtra("category", transaction.category)
            }
            startActivity(intent)
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
                filterTransactions(currentQuery) // preserve filters after reload
            }
            .addOnFailureListener { e ->
                e.printStackTrace()
            }
    }

    private fun filterTransactions(query: String?) {
        currentQuery = query

        val typeFilter = spinnerType.selectedItem?.toString() ?: "All"
        val categoryFilter = spinnerCategory.selectedItem?.toString() ?: "All"
        val priceValues = priceSlider.values
        val minPrice = priceValues.getOrElse(0) { 0f }  // safe fallback
        val maxPrice = priceValues.getOrElse(1) { priceSlider.valueTo } // safe fallback

        val filteredList = transactions.filter { tx ->
            // Text search
            val matchesQuery = query.isNullOrEmpty() ||
                    tx.name.contains(query, true) ||
                    tx.type.contains(query, true) ||
                    android.text.format.DateFormat.format("MMM dd, yyyy", tx.date)
                        .toString()
                        .contains(query, true)

            // Type filter
            val matchesType = typeFilter == "All" || tx.type == typeFilter

            // Category filter (only applies to Expense)
            val matchesCategory = categoryFilter == "All" ||
                    (tx.type == "Expense" && tx.category == categoryFilter)

            // Price filter
            val matchesPrice = tx.cost in minPrice..maxPrice

            // Date filter (exact match day)
            val matchesDate = selectedDate?.let {
                val calTx = Calendar.getInstance().apply { timeInMillis = tx.date }
                val calSel = Calendar.getInstance().apply { timeInMillis = it }
                calTx.get(Calendar.YEAR) == calSel.get(Calendar.YEAR) &&
                        calTx.get(Calendar.MONTH) == calSel.get(Calendar.MONTH) &&
                        calTx.get(Calendar.DAY_OF_MONTH) == calSel.get(Calendar.DAY_OF_MONTH)
            } ?: true

            matchesQuery && matchesType && matchesCategory && matchesPrice && matchesDate
        }

        transactionAdapter.updateList(filteredList, query)
    }

    override fun onResume() {
        super.onResume()
        loadTransactions()
    }
}




