package com.sortoapps.chorepilot.core.platform

import platform.UIKit.UIImpactFeedbackGenerator
import platform.UIKit.UIImpactFeedbackStyle
import platform.UIKit.UINotificationFeedbackGenerator
import platform.UIKit.UINotificationFeedbackType

class VibratorUtilImpl : VibratorUtil {
    override fun triggerVibration() {
        val impactGenerator = UIImpactFeedbackGenerator(
            style = UIImpactFeedbackStyle.UIImpactFeedbackStyleMedium,
        )
        impactGenerator.prepare()
        impactGenerator.impactOccurred()
    }

    override fun triggerVibrationSuccess() {
        val notificationGenerator = UINotificationFeedbackGenerator()
        notificationGenerator.prepare()
        notificationGenerator.notificationOccurred(
            UINotificationFeedbackType.UINotificationFeedbackTypeSuccess,
        )
    }

    override fun cancelVibration() = Unit
}
