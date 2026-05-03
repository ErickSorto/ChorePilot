import Foundation
import UserNotifications
import ComposeApp

final class NotificationCenterDelegate: NSObject, UNUserNotificationCenterDelegate {
    static let shared = NotificationCenterDelegate()
    private let notificationIdKey = "notificationId"
    private let choreIdKey = "choreId"
    private let notificationTypeKey = "type"

    private var isStarted = false

    private override init() {}

    func start() {
        guard !isStarted else { return }
        isStarted = true
        UNUserNotificationCenter.current().delegate = self
    }

    func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        willPresent notification: UNNotification,
        withCompletionHandler completionHandler: @escaping (UNNotificationPresentationOptions) -> Void
    ) {
        completionHandler([.banner, .sound, .list])
    }

    func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        didReceive response: UNNotificationResponse,
        withCompletionHandler completionHandler: @escaping () -> Void
    ) {
        let userInfo = response.notification.request.content.userInfo
        let notificationId = userInfo[notificationIdKey] as? String ?? ""
        let choreId = userInfo[choreIdKey] as? String ?? ""
        let type = userInfo[notificationTypeKey] as? String ?? ""
        NotificationTapBridgeKt.handleNotificationTap(
            notificationId: notificationId,
            choreId: choreId,
            type: type
        )
        completionHandler()
    }
}
