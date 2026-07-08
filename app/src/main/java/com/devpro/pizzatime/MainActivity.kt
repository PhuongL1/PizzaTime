package com.devpro.pizzatime

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.devpro.pizzatime.core.notification.OrderNotificationMonitor
import com.devpro.pizzatime.databinding.ActivityMainBinding
import com.devpro.pizzatime.feature.customer.cart.CartStore
import com.devpro.pizzatime.feature.splash.SplashFragment

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val systemBarColor = ContextCompat.getColor(this, R.color.pt_background)
        window.statusBarColor = systemBarColor
        window.navigationBarColor = systemBarColor
        CartStore.init(applicationContext)
        OrderNotificationMonitor.init(applicationContext)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, SplashFragment())
                .commit()
        }
    }

    override fun onResume() {
        super.onResume()
        OrderNotificationMonitor.setForegroundActivity(this)
    }

    override fun onPause() {
        OrderNotificationMonitor.setForegroundActivity(null)
        super.onPause()
    }
}
