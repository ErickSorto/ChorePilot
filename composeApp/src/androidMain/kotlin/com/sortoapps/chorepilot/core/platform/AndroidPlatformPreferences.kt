package com.sortoapps.chorepilot.core.platform

import android.content.Context

class AndroidPlatformPreferences(
    context: Context,
) : PlatformPreferences {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    override fun getString(key: String): String? = preferences.getString(key, null)

    override fun putString(key: String, value: String) {
        preferences.edit().putString(key, value).apply()
    }

    override fun remove(key: String) {
        preferences.edit().remove(key).apply()
    }

    private companion object {
        const val PREFERENCES_NAME = "chorepilot_demo_preferences"
    }
}
