package com.example.budguette

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore

class TransactionDetailActivity : AppCompatActivity() {

    private lateinit var nameTextView: TextView
    private lateinit var typeTextView: TextView
    private lateinit var dateTextView: TextView
    private lateinit var costTextView: TextView
    private lateinit var notesEditText: EditText
    private lateinit var saveNotesButton: Button
    private lateinit var deleteButton: Button
    private lateinit var categoryTextView: TextView
    private lateinit var transactionId: String
    private lateinit var userId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_transaction_detail)

        // Initialize views
        nameTextView = findViewById(R.id.nameTextView)
        typeTextView = findViewById(R.id.typeTextView)
        dateTextView = findViewById(R.id.dateTextView)
        costTextView = findViewById(R.id.costTextView)
        notesEditText = findViewById(R.id.notesEditText)
        saveNotesButton = findViewById(R.id.saveNotesButton)
        deleteButton = findViewById(R.id.deleteButton)
        categoryTextView = findViewById(R.id.categoryTextView)

        // Get intent extras
        transactionId = intent.getStringExtra("transactionId") ?: ""
        userId = intent.getStringExtra("userId") ?: ""

        val name = intent.getStringExtra("name")
        val type = intent.getStringExtra("type")
        val date = intent.getStringExtra("date")
        val cost = intent.getDoubleExtra("cost", 0.0)
        val notes = intent.getStringExtra("notes") ?: ""
        val category = intent.getStringExtra("category")

        // Set data to views
        nameTextView.text = "Name: $name"
        typeTextView.text = "Type: $type"
        dateTextView.text = "Date: $date"
        costTextView.text = "Cost: $${String.format("%.2f", cost)}"
        notesEditText.setText(notes)

        // Show category if it's an expense
        if (type == "Expense" && !category.isNullOrEmpty()) {
            categoryTextView.visibility = View.VISIBLE
            categoryTextView.text = "Category: $category"
        } else {
            categoryTextView.visibility = View.GONE
        }

        saveNotesButton.setOnClickListener {
            saveNotes()
        }

        deleteButton.setOnClickListener {
            showDeleteConfirmationDialog()
        }
    }

    private fun saveNotes() {
        val newNotes = notesEditText.text.toString()
        val db = FirebaseFirestore.getInstance()
        db.collection("users")
            .document(userId)
            .collection("transactions")
            .document(transactionId)
            .update("notes", newNotes)
            .addOnSuccessListener {
                Toast.makeText(this, "Notes saved!", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to save notes.", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showDeleteConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle("Delete Transaction")
            .setMessage("Are you sure you want to delete this transaction?")
            .setPositiveButton("Yes") { _, _ ->
                deleteTransaction()
            }
            .setNegativeButton("No", null)
            .show()
    }

    private fun deleteTransaction() {
        val db = FirebaseFirestore.getInstance()
        db.collection("users")
            .document(userId)
            .collection("transactions")
            .document(transactionId)
            .delete()
            .addOnSuccessListener {
                Toast.makeText(this, "Transaction deleted", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to delete", Toast.LENGTH_SHORT).show()
            }
    }
}


