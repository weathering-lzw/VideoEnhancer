package com.videoenhancer

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.videoenhancer.service.FloatingBubbleService

class App : Application() {

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        val channel = NotificationChannel(
            FloatingBubbleService.CHANNEL_ID,
            getString(R.string.channel_bubble),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = getString(R.string.channel_bubble_desc)
            setShowBadge(false)
        }

        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }
}
