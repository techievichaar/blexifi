package com.blexifi.app.ops

import android.content.Context

object HealthReporter {
    private const val PREF = "blexifi_health"

    fun increment(context: Context, key: String) {
        val prefs = context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
        val value = prefs.getInt(key, 0) + 1
        prefs.edit().putInt(key, value).apply()
    }

    fun setLastTimestamp(context: Context, key: String, nowMs: Long) {
        val prefs = context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
        prefs.edit().putLong(key, nowMs).apply()
    }
}
