package com.sortoapps.chorepilot.core.platform

interface PlatformPreferences {
    fun getString(key: String): String?

    fun putString(key: String, value: String)

    fun remove(key: String)
}
