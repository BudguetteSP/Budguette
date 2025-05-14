package com.example.budguette

import android.content.DialogInterface
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class SubscriptionDetailActivity : AppCompatActivity() {

    private lateinit var nameTextView: TextView
    private lateinit var amountTextView: TextView
    private lateinit var frequencyTextView: TextView
    private lateinit var startDateTextView: TextView
    private lateinit var notesEditText: EditText
    private lateinit var saveNotesButton: Button
    private lateinit var deleteButton: Button

    private val db by lazy { FirebaseFirestore.getInstance() }
    private val auth by lazy { FirebaseAuth.getInstance() }

    private var subscription: Subscription? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_subscription_detail)

        // Initialize views
        nameTextView = findViewById(R.id.detailName)
        amountTextView = findViewById(R.id.detailAmount)
        frequencyTextView = findViewById(R.id.detailFrequency)
        startDateTextView = findViewById(R.id.detailStartDate)
        notesEditText = findViewById(R.id.detailNotesEdit)
        saveNotesButton = findViewById(R.id.saveNotesButton)
        deleteButton = findViewById(R.id.deleteSubscriptionButton)

        // Get subscription from intent
        subscription = intent.getSerializableExtra("subscription") as? Subscription
        subscription?.let { sub ->
            nameTextView.text = sub.name
            amountTextView.text = "Amount: $${sub.amount}"
            frequencyTextView.text = "Frequency: ${sub.frequency}"
            startDateTextView.text = "Start Date: ${sub.startDate}"
            notesEditText.setText(sub.notes)
        }

        saveNotesButton.setOnClickListener { updateNotes() }
        deleteButton.setOnClickListener { confirmDelete() }
    }

    private fun updateNotes() {
        val sub = subscription ?: return
        val updatedNotes = notesEditText.text.toString().trim()
        db.collection("subscriptions")
            .document(sub.id)
            .update("notes", updatedNotes)
            .addOnSuccessListener {
                Toast.makeText(this, "Notes updated", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Update failed", Toast.LENGTH_SHORT).show()
            }
    }

    private fun confirmDelete() {
        AlertDialog.Builder(this)
            .setTitle("Delete Subscription")
            .setMessage("Are you sure you want to delete this subscription?")
            .setPositiveButton("Delete") { _, _ -> deleteSubscription() }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteSubscription() {
        val sub = subscription ?: return
        db.collection("subscriptions")
            .document(sub.id)
            .delete()
            .addOnSuccessListener {
                Toast.makeText(this, "Subscription deleted", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Delete failed", Toast.LENGTH_SHORT).show()
            }
    }
}
