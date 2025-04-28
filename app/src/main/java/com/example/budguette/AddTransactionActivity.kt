package com.example.budguette

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.*

class AddTransactionActivity : AppCompatActivity() {

    private lateinit var typeSpinner: Spinner
    private lateinit var categorySpinner: Spinner
    private lateinit var dateButton: Button
    private lateinit var costEditText: EditText
    private lateinit var saveButton: Button
    private lateinit var nameEditText: EditText

    private var selectedDate: Long = System.currentTimeMillis()
    private var selectedCategory: String? = null

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_transaction)

        typeSpinner = findViewById(R.id.typeSpinner)
        categorySpinner = findViewById(R.id.categorySpinner)
        dateButton = findViewById(R.id.dateButton)
        costEditText = findViewById(R.id.costEditText)
        saveButton = findViewById(R.id.saveButton)
        nameEditText = findViewById(R.id.nameEditText)

        val typeAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, listOf("Deposit", "Expense"))
        typeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        typeSpinner.adapter = typeAdapter

        val categoryAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, listOf("Transportation", "Food", "Rent"))
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        categorySpinner.adapter = categoryAdapter

        // Type selection logic
        typeSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedType = typeSpinner.selectedItem.toString()
                if (selectedType == "Expense") {
                    categorySpinner.visibility = View.VISIBLE
                } else {
                    categorySpinner.visibility = View.GONE
                    selectedCategory = null // Clear selected category if not Expense
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // Category selection logic
        categorySpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedCategory = categorySpinner.selectedItem.toString()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        dateButton.setOnClickListener {
            openDatePicker()
        }

        saveButton.setOnClickListener {
            saveTransaction()
        }
    }

    private fun openDatePicker() {
        val calendar = Calendar.getInstance()
        val dialog = DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                calendar.set(year, month, dayOfMonth)
                selectedDate = calendar.timeInMillis
                dateButton.text = "${month + 1}/$dayOfMonth/$year"
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        dialog.show()
    }

    private fun saveTransaction() {
        val userId = auth.currentUser?.uid ?: return
        val type = typeSpinner.selectedItem.toString()
        val cost = costEditText.text.toString().toDoubleOrNull()
        val name = nameEditText.text.toString()

        if (cost == null || name.isEmpty()) {
            Toast.makeText(this, "Please enter valid cost and name", Toast.LENGTH_SHORT).show()
            return
        }

        val transactionMap = mutableMapOf<String, Any>(
            "id" to UUID.randomUUID().toString(),
            "type" to type,
            "name" to name,
            "date" to selectedDate,
            "cost" to cost
        )

        // Only add category if type is Expense
        if (type == "Expense" && selectedCategory != null) {
            transactionMap["category"] = selectedCategory!!
        }

        db.collection("users").document(userId).collection("transactions")
            .document(transactionMap["id"].toString())
            .set(transactionMap)
            .addOnSuccessListener {
                Toast.makeText(this, "Transaction saved", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error saving transaction", Toast.LENGTH_SHORT).show()
            }
    }
}

