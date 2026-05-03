import SwiftUI
import GoogleSignIn

@main
struct iOSApp: App {
    @UIApplicationDelegateAdaptor(PushMessagingAppDelegate.self) var pushMessagingAppDelegate

    init() {
        GoogleSignInBridge.shared.start()
        ProofCameraBridge.shared.start()
        NotificationCenterDelegate.shared.start()
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
                .onOpenURL { url in
                    _ = GoogleSignInBridge.shared.handle(url: url)
                }
        }
    }
}
