import Foundation
import GoogleSignIn
import UIKit

private let googleSignInRequestNotification = Notification.Name("ChorePilotGoogleSignInRequested")
private let googleSignInSuccessNotification = Notification.Name("ChorePilotGoogleSignInSucceeded")
private let googleSignInFailureNotification = Notification.Name("ChorePilotGoogleSignInFailed")

final class GoogleSignInBridge {
    static let shared = GoogleSignInBridge()

    private var isStarted = false

    private init() {}

    func start() {
        guard !isStarted else { return }
        isStarted = true
        NotificationCenter.default.addObserver(
            self,
            selector: #selector(handleGoogleSignInRequest),
            name: googleSignInRequestNotification,
            object: nil
        )
    }

    @objc
    private func handleGoogleSignInRequest() {
        guard let presentingViewController = topViewController() else {
            postFailure("Google sign-in needs an active iOS screen.")
            return
        }

        guard
            let googleServicePath = Bundle.main.path(forResource: "GoogleService-Info", ofType: "plist"),
            let googleServiceInfo = NSDictionary(contentsOfFile: googleServicePath),
            let clientID = googleServiceInfo["CLIENT_ID"] as? String
        else {
            postFailure("Google sign-in isn't configured on iOS yet. Add GoogleService-Info.plist to iosApp/iosApp.")
            return
        }

        GIDSignIn.sharedInstance.configuration = GIDConfiguration(clientID: clientID)
        GIDSignIn.sharedInstance.signIn(withPresenting: presentingViewController) { result, error in
            if let error {
                self.postFailure(error.localizedDescription)
                return
            }

            guard
                let user = result?.user,
                let idToken = user.idToken?.tokenString
            else {
                self.postFailure("Google sign-in did not return an ID token.")
                return
            }

            NotificationCenter.default.post(
                name: googleSignInSuccessNotification,
                object: nil,
                userInfo: [
                    "idToken": idToken,
                    "accessToken": user.accessToken.tokenString,
                    "displayName": user.profile?.name ?? "",
                    "email": user.profile?.email ?? "",
                ]
            )
        }
    }

    func handle(url: URL) -> Bool {
        GIDSignIn.sharedInstance.handle(url)
    }

    private func postFailure(_ message: String) {
        NotificationCenter.default.post(
            name: googleSignInFailureNotification,
            object: nil,
            userInfo: ["message": message]
        )
    }

    private func topViewController(
        from root: UIViewController? = UIApplication.shared.connectedScenes
            .compactMap { $0 as? UIWindowScene }
            .flatMap(\.windows)
            .first(where: \.isKeyWindow)?
            .rootViewController
    ) -> UIViewController? {
        if let navigationController = root as? UINavigationController {
            return topViewController(from: navigationController.visibleViewController)
        }
        if let tabBarController = root as? UITabBarController {
            return topViewController(from: tabBarController.selectedViewController)
        }
        if let presentedViewController = root?.presentedViewController {
            return topViewController(from: presentedViewController)
        }
        return root
    }
}
