package com.sortoapps.chorepilot.core.platform

import platform.Foundation.NSUserDefaults

class IosPlatformPreferences : PlatformPreferences {
    private val defaults = NSUserDefaults.standardUserDefaults

    override fun getString(key: String): String? = defaults.stringForKey(key)

    override fun putString(key: String, value: String) {
        defaults.setObject(value, key)
    }

    override fun remove(key: String) {
        defaults.removeObjectForKey(key)
    }
}
