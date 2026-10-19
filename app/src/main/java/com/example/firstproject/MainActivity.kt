package com.example.firstproject

import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.example.firstproject.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    val TAG = "MainActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Inflate binding instead of setContentView with layout
        val binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
// Get NavController from NavHostFragment
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController
// Define top-level destinations for ActionBar
        val appBarConfiguration = AppBarConfiguration(
            setOf(R.id.homeFragment, R.id.profileFragment)
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
// Hook up BottomNavigationView via binding
        binding.bottomNav.setupWithNavController(navController)

        Log.d(TAG, "onCreate: MainActivity called.")
    }

    override fun onStart() {
        super.onStart()
        Log.d(TAG, "onStart: MainActivity called.")
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume: MainActivity called.")
    }

    override fun onPause() {
        super.onPause()
        Log.d(TAG, "onPause: MainActivity called.")
    }

    override fun onStop() {
        super.onStop()
        Log.d(TAG, "onStop: MainActivity called.")
    }

    override fun onRestart() {
        super.onRestart()
        Log.d(TAG, "onRestart: MainActivity called.")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy: MainActivity called.")
    }
}