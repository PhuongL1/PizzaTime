package com.devpro.pizzatime

import android.app.Application
import com.devpro.pizzatime.shared.location.OsmdroidConfiguration

class PizzaTimeApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        OsmdroidConfiguration.configure(
            context = applicationContext,
            applicationId = BuildConfig.APPLICATION_ID,
        )
    }
}
