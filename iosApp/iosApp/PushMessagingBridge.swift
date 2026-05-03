import Foundation
import UIKit
import UserNotifications
import FirebaseMessaging

final class PushMessagingBridge: NSObject, MessagingDelegate {
    static let shared = PushMessagingBridge()

    private let tokenKey = "push.fcm_token"
    private let tokenChangedNotification = Notification.Name("ChorePilotPushTokenDidChange")
    private var didActivateMessaging = false

    private override init() {}

    func activateMessagingIfNeeded() {
        guard !didActivateMessaging else { return }
        didActivateMessaging = true
        Messaging.messaging().delegate = self
        DispatchQueue.main.async {
            UIApplication.shared.registerForRemoteNotifications()
        }
        Messaging.messaging().token { [weak self] token, _ in
            guard let self, let token, !token.isEmpty else { return }
            self.persistToken(token)
        }
    }

    func application(_ application: UIApplication, didRegisterForRemoteNotificationsWithDeviceToken deviceToken: Data) {
        Messaging.messaging().apnsToken = deviceToken
        activateMessagingIfNeeded()
    }

    func application(_ application: UIApplication, didFailToRegisterForRemoteNotificationsWithError error: Error) {
        print("ChorePilotPush: APNs registration failed: \(error.localizedDescription)")
    }

    func messaging(_ messaging: Messaging, didReceiveRegistrationToken fcmToken: String?) {
        guard let fcmToken, !fcmToken.isEmpty else { return }
        persistToken(fcmToken)
    }

    private func persistToken(_ token: String) {
        let defaults = UserDefaults.standard
        if defaults.string(forKey: tokenKey) == token { return }
        defaults.set(token, forKey: tokenKey)
        NotificationCenter.default.post(name: tokenChangedNotification, object: nil)
    }
}

final class PushMessagingAppDelegate: NSObject, UIApplicationDelegate {
    func application(
        _ application: UIApplication,
        didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]? = nil
    ) -> Bool {
        true
    }

    func application(_ application: UIApplication, didRegisterForRemoteNotificationsWithDeviceToken deviceToken: Data) {
        PushMessagingBridge.shared.application(application, didRegisterForRemoteNotificationsWithDeviceToken: deviceToken)
    }

    func application(_ application: UIApplication, didFailToRegisterForRemoteNotificationsWithError error: Error) {
        PushMessagingBridge.shared.application(application, didFailToRegisterForRemoteNotificationsWithError: error)
    }
}
