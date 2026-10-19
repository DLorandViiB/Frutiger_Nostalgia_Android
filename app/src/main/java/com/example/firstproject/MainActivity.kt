package com.example.firstproject

import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.example.firstproject.databinding.ActivityMainBinding
import com.example.firstproject.utils.SessionManager

class MainActivity : AppCompatActivity() {
    val TAG = "MainActivity"

    // Destinations shown without the toolbar and bottom navigation
    private val authDestinations = setOf(R.id.loginFragment, R.id.registerFragment)

    private val localNetworkPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            Log.d(TAG, "Local network permission granted: $granted")
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Light status-bar icons over the deep blue sky, dark nav-bar icons over the glass bar
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
        )
        super.onCreate(savedInstanceState)
        // Inflate binding instead of setContentView with layout
        val binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        requestLocalNetworkAccessForDevBackend()
// Get NavController from NavHostFragment
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController
        // Skip login when a token is already saved. On recreation setGraph restores the saved back stack.
        val navGraph = navController.navInflater.inflate(R.navigation.nav_graph).apply {
            setStartDestination(
                if (SessionManager(this@MainActivity).isLoggedIn()) R.id.homeFragment
                else R.id.loginFragment
            )
        }
        navController.setGraph(navGraph, null)
// Define top-level destinations for ActionBar
        val appBarConfiguration = AppBarConfiguration(
            setOf(R.id.homeFragment, R.id.profileFragment)
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
// Hook up BottomNavigationView via binding
        binding.bottomNav.setupWithNavController(navController)
        // Keep the glossy orb icons' own colours (a null tint can't be set from a style)
        binding.bottomNav.itemIconTintList = null

        // Hide the toolbar and bottom nav on the auth screens
        navController.addOnDestinationChangedListener { _, destination, _ ->
            val showChrome = destination.id !in authDestinations
            binding.toolbar.isVisible = showChrome
            binding.bottomNav.isVisible = showChrome
            ViewCompat.requestApplyInsets(binding.contentRoot)
        }

        // Edge-to-edge insets: content sits below the status bar; the bottom nav pads itself for the
        // navigation bar, so the content only needs bottom padding (nav bar or keyboard) when it's hidden.
        ViewCompat.setOnApplyWindowInsetsListener(binding.contentRoot) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val ime = insets.getInsets(WindowInsetsCompat.Type.ime())
            view.updatePadding(
                left = bars.left,
                top = bars.top,
                right = bars.right,
                bottom = if (binding.bottomNav.isVisible) 0 else maxOf(bars.bottom, ime.bottom)
            )
            insets
        }

        Log.d(TAG, "onCreate: MainActivity called.")
    }

    /**
     * Android 17 (API 37) blocks private-range addresses like the emulator's 10.0.2.2 unless the app holds
     * ACCESS_LOCAL_NETWORK; without it requests just time out. Only the debug build talks to a local backend,
     * so only the debug manifest declares the permission and only debug builds ask for it.
     */
    private fun requestLocalNetworkAccessForDevBackend() {
        if (!BuildConfig.DEBUG || Build.VERSION.SDK_INT < 37) return
        if (ContextCompat.checkSelfPermission(this, PERMISSION_LOCAL_NETWORK) != PackageManager.PERMISSION_GRANTED) {
            localNetworkPermissionLauncher.launch(PERMISSION_LOCAL_NETWORK)
        }
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

    private companion object {
        // String literal: the Manifest.permission constant only exists when compiling against API 37+
        const val PERMISSION_LOCAL_NETWORK = "android.permission.ACCESS_LOCAL_NETWORK"
    }
}
