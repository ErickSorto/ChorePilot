package com.sortoapps.chorepilot.core.platform

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.annotation.RequiresPermission

class VibratorUtilImpl(
    private val context: Context,
) : VibratorUtil {
    @RequiresPermission(android.Manifest.permission.VIBRATE)
    override fun triggerVibration() {
        val vibrator = context.resolveVibrator() ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            vibrator.vibrate(
                VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK),
            )
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(60L)
        }
    }

    @RequiresPermission(android.Manifest.permission.VIBRATE)
    override fun triggerVibrationSuccess() {
        val vibrator = context.resolveVibrator() ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            vibrator.vibrate(
                VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK),
            )
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(longArrayOf(0L, 70L, 45L, 90L), -1)
        }
    }

    override fun cancelVibration() {
        context.resolveVibrator()?.cancel()
    }
}

private fun Context.resolveVibrator(): Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
    (getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
} else {
    @Suppress("DEPRECATION")
    getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
}
