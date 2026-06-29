package com.devpro.pizzatime

import android.graphics.Color
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.devpro.pizzatime.databinding.ActivityMainBinding
import com.devpro.pizzatime.feature.splash.SplashFragment

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.statusBarColor = Color.parseColor("#151312")
        window.navigationBarColor = Color.parseColor("#151312")

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, SplashFragment())
                .commit()
        }
    }
}