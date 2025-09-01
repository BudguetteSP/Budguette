package com.example.budguette

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.prolificinteractive.materialcalendarview.CalendarDay
import com.prolificinteractive.materialcalendarview.MaterialCalendarView
import com.prolificinteractive.materialcalendarview.DayViewDecorator
import com.prolificinteractive.materialcalendarview.DayViewFacade
import java.text.SimpleDateFormat
import java.util.*

class CalendarFragment : Fragment() {

    private lateinit var calendarView: MaterialCalendarView
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_calendar, container, false)
        calendarView = view.findViewById(R.id.calendarView)

        loadSubscriptions()

        return view
    }

    private fun loadSubscriptions() {
        // Example subscriptions — replace with Firebase query in your app
        val subscriptions = listOf(
            Subscription("1", "userId", "Netflix", 12.99, "Monthly", "2025-09-05", ""),
            Subscription("2", "userId", "Spotify", 9.99, "Weekly", "2025-09-03", "")
        )

        val events = mutableListOf<CalendarDay>()

        for (sub in subscriptions) {
            val startDate: Date = try {
                dateFormat.parse(sub.startDate) ?: continue
            } catch (e: Exception) {
                continue
            }

            when (sub.frequency.lowercase()) {
                "one-time" -> events.add(CalendarDay.from(startDate))
                "weekly" -> {
                    val cal = Calendar.getInstance().apply { time = startDate }
                    for (i in 0..4) { // next 5 weeks
                        events.add(CalendarDay.from(cal))
                        cal.add(Calendar.WEEK_OF_YEAR, 1)
                    }
                }
                "monthly" -> {
                    val cal = Calendar.getInstance().apply { time = startDate }
                    for (i in 0..11) { // next 12 months
                        events.add(CalendarDay.from(cal))
                        cal.add(Calendar.MONTH, 1)
                    }
                }
                "yearly" -> {
                    val cal = Calendar.getInstance().apply { time = startDate }
                    for (i in 0..4) { // next 5 years
                        events.add(CalendarDay.from(cal))
                        cal.add(Calendar.YEAR, 1)
                    }
                }
            }
        }

        // Decorate the calendar with dots
        calendarView.addDecorator(EventDecorator(events))
    }
}

/** Simple decorator to highlight dates */
class EventDecorator(private val dates: Collection<CalendarDay>) : DayViewDecorator {

    override fun shouldDecorate(day: CalendarDay): Boolean {
        return dates.contains(day)
    }

    override fun decorate(view: DayViewFacade) {
        view.addSpan(com.prolificinteractive.materialcalendarview.spans.DotSpan(8f, Color.RED))
    }
}
