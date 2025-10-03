package com.example.budguette

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.widget.ListView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.FirebaseApp
import com.jakewharton.threetenabp.AndroidThreeTen

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseApp.initializeApp(this)
        AndroidThreeTen.init(this)
        setContentView(R.layout.activity_main)

        createNotificationChannel()

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation)


        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> loadFragment(HomeFragment())
                R.id.nav_expenses -> loadFragment(ExpensesFragment())
                R.id.nav_subscriptions -> loadFragment(SubscriptionsFragment())
                R.id.nav_calendar -> loadFragment(CalendarFragment())
                R.id.nav_more -> showMoreMenu()
            }
            true
        }

        if (savedInstanceState == null) {
            bottomNav.selectedItemId = R.id.nav_home
        }
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }

    private fun showMoreMenu() {
        val options = listOf(
            "Profile" to R.drawable.ic_profile,
            "Forums" to R.drawable.ic_forums
        )

        val bottomSheetDialog = com.google.android.material.bottomsheet.BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.bottom_sheet_more, null)
        val listView = view.findViewById<ListView>(R.id.moreListView)

        val adapter = MoreMenuAdapter(this, options)
        listView.adapter = adapter

        listView.setOnItemClickListener { _, _, position, _ ->
            when (options[position].first) {
                "Profile" -> loadFragment(ProfileFragment())
                "Forums" -> loadFragment(ForumsFragment())
            }
            bottomSheetDialog.dismiss()
        }

        bottomSheetDialog.setContentView(view)
        bottomSheetDialog.show()
    }

    // 🔔 Create notification channel
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "subscription_reminders",
                "Subscription Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alerts for upcoming subscriptions"
            }

            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }


}


