package com.futureape.kanleme

import android.app.Application
import android.content.Context
import androidx.work.Configuration
import androidx.work.WorkManager
import androidx.test.runner.AndroidJUnitRunner
import dagger.hilt.android.testing.HiltTestApplication

class HiltTestRunner : AndroidJUnitRunner() {
    override fun newApplication(
        cl: ClassLoader?,
        name: String?,
        context: Context?,
    ): Application = super.newApplication(cl, HiltTestApplication::class.java.name, context)

    override fun onStart() {
        val targetApplicationContext = targetContext.applicationContext
        try {
            WorkManager.getInstance(targetApplicationContext)
        } catch (_: IllegalStateException) {
            WorkManager.initialize(
                targetApplicationContext,
                Configuration.Builder().setMinimumLoggingLevel(android.util.Log.DEBUG).build(),
            )
        }
        super.onStart()
    }
}
