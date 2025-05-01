package com.example.budguette

import android.app.DatePickerDialog
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class AddSubscriptionActivity : AppCompatActivity() {

    private lateinit var nameEditText: EditText
    private lateinit var amountEditText: EditText
    private lateinit var frequencySpinner: Spinner
    private lateinit var selectDateButton: Button
    private lateinit var notesEditText: EditText
    private lateinit var saveButton: Button

    private val auth by lazy { FirebaseAuth.getInstance() }
    private val db by lazy { FirebaseFirestore.getInstance() }

    // Will hold the selected date in "yyyy-MM-dd" format
    private var selectedDate: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_subscription)

        // Wire up views
        nameEditText       = findViewById(R.id.subscriptionNameEditText)
        amountEditText     = findViewById(R.id.subscriptionCostEditText)
        frequencySpinner   = findViewById(R.id.subscriptionFrequencySpinner)
        selectDateButton   = findViewById(R.id.selectDateButton)
        notesEditText      = findViewById(R.id.subscriptionNotesEditText)
        saveButton         = findViewById(R.id.saveSubscriptionButton)

        // Populate the frequency dropdown
        ArrayAdapter.createFromResource(
            this,
            R.array.subscription_frequencies,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            frequencySpinner.adapter = adapter
        }

        // Date picker opens calendar and sets button text
        selectDateButton.setOnClickListener {
            showDatePicker()
        }

        // Save button logic
        saveButton.setOnClickListener {
            saveSubscription()
        }
    }

    private fun showDatePicker() {
        val now = Calendar.getInstance()
        DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                // Format as yyyy-MM-dd
                Calendar.getInstance().apply {
                    set(year, month, dayOfMonth)
                }.time.let { date ->
                    selectedDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(date)
                    selectDateButton.text = selectedDate
                }
            },
            now.get(Calendar.YEAR),
            now.get(Calendar.MONTH),
            now.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun saveSubscription() {
        val user = auth.currentUser
        if (user == null) {
            Toast.makeText(this, "You must be logged in", Toast.LENGTH_SHORT).show()
            return
        }

        // Gather inputs
        val name      = nameEditText.text.toString().trim()
        val amountVal = amountEditText.text.toString().toDoubleOrNull()
        val frequency = frequencySpinner.selectedItem.toString()
        val notes     = notesEditText.text.toString().trim()

        // Validate
        if (name.isEmpty() || amountVal == null) {
            Toast.makeText(this, "Enter a valid name and amount", Toast.LENGTH_SHORT).show()
            return
        }
        if (selectedDate.isEmpty()) {
            Toast.makeText(this, "Please select a start date", Toast.LENGTH_SHORT).show()
            return
        }

        // Build model
        val id = UUID.randomUUID().toString()
        val sub = Subscription(
            id = id,
            userId = user.uid,
            name = name,
            amount = amountVal,
            frequency = frequency,
            startDate = selectedDate,
            notes = notes
        )

        // Save to Firestore
        db.collection("subscriptions")
            .document(id)
            .set(sub)
            .addOnSuccessListener {
                Toast.makeText(this, "Subscription saved", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }
}


