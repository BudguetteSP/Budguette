// AddTransactionActivity.kt
package com.example.budguette

import android.app.DatePickerDialog
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.*

class AddTransactionActivity : AppCompatActivity() {

    private lateinit var typeSpinner: Spinner
    private lateinit var dateButton: Button
    private lateinit var costEditText: EditText
    private lateinit var saveButton: Button
    private lateinit var nameEditText: EditText


    private var selectedDate: Long = System.currentTimeMillis()

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_transaction)

        typeSpinner = findViewById(R.id.typeSpinner)
        dateButton = findViewById(R.id.dateButton)
        costEditText = findViewById(R.id.costEditText)
        saveButton = findViewById(R.id.saveButton)
        nameEditText = findViewById(R.id.nameEditText)


        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, listOf("Deposit", "Expense"))
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        typeSpinner.adapter = adapter

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

        val newTransaction = Transaction(
            id = UUID.randomUUID().toString(),
            type = type,
            name = name,
            date = selectedDate,
            cost = cost
        )

        db.collection("users").document(userId).collection("transactions")
            .document(newTransaction.id)
            .set(newTransaction)
            .addOnSuccessListener {
                Toast.makeText(this, "Transaction saved", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error saving transaction", Toast.LENGTH_SHORT).show()
            }
    }
}
