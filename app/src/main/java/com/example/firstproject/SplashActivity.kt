package com.example.firstproject

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.example.firstproject.databinding.ActivitySplashBinding

class SplashActivity : AppCompatActivity() {
    val TAG = "SplashActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
        )

        val binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val basePadding = binding.contentRoot.paddingTop
        ViewCompat.setOnApplyWindowInsetsListener(binding.contentRoot) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.updatePadding(
                left = basePadding + bars.left,
                top = basePadding + bars.top,
                right = basePadding + bars.right,
                bottom = basePadding + bars.bottom
            )
            insets
        }

        binding.button.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.putExtra("message", "Hello from Splash!")
            Log.d(TAG, "Button Clicked.")
            startActivity(intent)
        }

        Log.d(TAG, "onCreate: SplashActivity called.")
    }

    override fun onStart() {
        super.onStart()
        Log.d(TAG, "onStart: SplashActivity started.")
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume: SplashActivity called.")
    }

    override fun onPause() {
        super.onPause()
        Log.d(TAG, "onPause: SplashActivity called.")
    }

    override fun onStop() {
        super.onStop()
        Log.d(TAG, "onStop: SplashActivity called.")
    }

    override fun onRestart() {
        super.onRestart()
        Log.d(TAG, "onRestart: SplashActivity called.")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy: SplashActivity called.")
    }
}
