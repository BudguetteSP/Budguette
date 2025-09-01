package com.example.budguette

import android.os.Bundle
import android.view.MenuItem
import android.widget.PopupMenu
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

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation)

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> loadFragment(HomeFragment())
                R.id.nav_expenses -> loadFragment(ExpensesFragment())
                R.id.nav_subscriptions -> loadFragment(SubscriptionsFragment())
                R.id.nav_profile -> loadFragment(ProfileFragment())
                R.id.nav_more -> showMoreMenu(bottomNav, item)
            }
            true
        }

        // Load the default fragment
        if (savedInstanceState == null) {
            bottomNav.selectedItemId = R.id.nav_home
        }
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }

    private fun showMoreMenu(bottomNav: BottomNavigationView, anchorItem: MenuItem) {
        // Show a popup menu anchored to the "More" item
        val moreMenu = PopupMenu(this, bottomNav.findViewById(anchorItem.itemId))
        moreMenu.menu.add("Forums").setOnMenuItemClickListener {
            loadFragment(ForumsFragment())
            true
        }
        moreMenu.menu.add("Calendar").setOnMenuItemClickListener {
            loadFragment(CalendarFragment())
            true
        }
        moreMenu.show()
    }
}
