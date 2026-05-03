package com.sortoapps.chorepilot.core.platform

import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication
import platform.UIKit.UIPasteboard
import platform.UIKit.UIViewController

class IosSystemTextActions : SystemTextActions {
    override fun copyText(text: String) {
        UIPasteboard.generalPasteboard.string = text
    }

    override fun shareText(text: String) {
        val controller = UIActivityViewController(
            activityItems = listOf(text),
            applicationActivities = null,
        )
        topViewController()?.presentViewController(
            viewControllerToPresent = controller,
            animated = true,
            completion = null,
        )
    }

    private fun topViewController(): UIViewController? {
        var controller = UIApplication.sharedApplication.keyWindow?.rootViewController
        while (controller?.presentedViewController != null) {
            controller = controller.presentedViewController
        }
        return controller
    }
}
