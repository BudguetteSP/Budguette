package com.example.budguette

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.prolificinteractive.materialcalendarview.CalendarDay
import com.prolificinteractive.materialcalendarview.DayViewDecorator
import com.prolificinteractive.materialcalendarview.DayViewFacade
import com.prolificinteractive.materialcalendarview.MaterialCalendarView
import com.prolificinteractive.materialcalendarview.spans.DotSpan
import java.text.SimpleDateFormat
import java.util.*

class CalendarFragment : Fragment() {

    private lateinit var calendarView: MaterialCalendarView
    private val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val colors = mapOf(
        "Monthly" to 0xFFFF0000.toInt(),   // Red
        "Yearly" to 0xFF00FF00.toInt(),    // Green
        "Weekly" to 0xFF0000FF.toInt(),    // Blue
        "One-Time" to 0xFFFFA500.toInt()   // Orange
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_calendar, container, false)
        calendarView = view.findViewById(R.id.calendarView)
        fetchSubscriptionsAndDecorate()
        return view
    }

    private fun fetchSubscriptionsAndDecorate() {
        val userId = auth.currentUser?.uid ?: return

        db.collection("subscriptions")
            .whereEqualTo("userId", userId)
            .get()
            .addOnSuccessListener { snapshot ->
                val subscriptions = snapshot.documents.map { doc ->
                    Subscription(
                        id = doc.id,
                        userId = doc.getString("userId") ?: "",
                        name = doc.getString("name") ?: "",
                        amount = doc.getDouble("amount") ?: 0.0,
                        frequency = doc.getString("frequency") ?: "",
                        startDate = doc.getString("startDate") ?: "",
                        notes = doc.getString("notes") ?: ""
                    )
                }

                decorateCalendar(subscriptions)
            }
            .addOnFailureListener { e ->
                e.printStackTrace()
            }
    }

    private fun decorateCalendar(subscriptions: List<Subscription>) {
        val subsByDate = subscriptions.groupBy { it.startDate }

        // Add decorators
        subsByDate.forEach { (dateString, subs) ->
            val date = sdf.parse(dateString) ?: return@forEach
            val day = CalendarDay.from(date)

            subs.forEach { sub ->
                calendarView.addDecorator(object : DayViewDecorator {
                    override fun shouldDecorate(dayToCheck: CalendarDay): Boolean {
                        return dayToCheck == day
                    }

                    override fun decorate(view: DayViewFacade) {
                        view.addSpan(DotSpan(6f, colors[sub.frequency] ?: 0xFF000000.toInt()))
                    }
                })
            }
        }

        // Set day click listener
        calendarView.setOnDateChangedListener { widget, date, selected ->
            val clickedDate = sdf.format(date.date)
            val subsForDay = subsByDate[clickedDate]
            if (!subsForDay.isNullOrEmpty()) {
                val message = subsForDay.joinToString("\n") {
                    "${it.name} - $${it.amount} (${it.frequency})"
                }
                AlertDialog.Builder(requireContext())
                    .setTitle("Subscriptions on $clickedDate")
                    .setMessage(message)
                    .setPositiveButton("OK", null)
                    .show()
            }
        }
    }
}





