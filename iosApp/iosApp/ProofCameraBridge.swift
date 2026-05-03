import Foundation
import UIKit

private let proofCameraRequestNotification = Notification.Name("ChorePilotProofCameraRequested")
private let proofCameraSuccessNotification = Notification.Name("ChorePilotProofCameraSucceeded")
private let proofCameraFailureNotification = Notification.Name("ChorePilotProofCameraFailed")

final class ProofCameraBridge: NSObject, UIImagePickerControllerDelegate, UINavigationControllerDelegate {
    static let shared = ProofCameraBridge()

    private var isStarted = false
    private var pickerController: UIImagePickerController?

    private override init() {}

    func start() {
        guard !isStarted else { return }
        isStarted = true
        NotificationCenter.default.addObserver(
            self,
            selector: #selector(handleProofCameraRequest),
            name: proofCameraRequestNotification,
            object: nil
        )
    }

    @objc
    private func handleProofCameraRequest() {
        guard let presentingViewController = topViewController() else {
            postFailure("Camera capture needs an active iOS screen.")
            return
        }

        let picker = UIImagePickerController()
        picker.delegate = self
        picker.allowsEditing = false
        picker.sourceType = UIImagePickerController.isSourceTypeAvailable(.camera) ? .camera : .photoLibrary
        picker.modalPresentationStyle = .fullScreen
        pickerController = picker
        presentingViewController.present(picker, animated: true)
    }

    func imagePickerControllerDidCancel(_ picker: UIImagePickerController) {
        picker.dismiss(animated: true)
        pickerController = nil
        postFailure("Photo capture was cancelled.")
    }

    func imagePickerController(
        _ picker: UIImagePickerController,
        didFinishPickingMediaWithInfo info: [UIImagePickerController.InfoKey : Any]
    ) {
        defer {
            picker.dismiss(animated: true)
            pickerController = nil
        }

        guard let image = info[.originalImage] as? UIImage else {
            postFailure("The captured photo was empty.")
            return
        }

        guard let jpegData = image.jpegData(compressionQuality: 0.9) else {
            postFailure("We couldn't prepare that photo.")
            return
        }

        let fileUrl = URL(fileURLWithPath: NSTemporaryDirectory())
            .appendingPathComponent("proof-\(UUID().uuidString).jpg")

        do {
            try jpegData.write(to: fileUrl, options: .atomic)
            NotificationCenter.default.post(
                name: proofCameraSuccessNotification,
                object: nil,
                userInfo: ["path": fileUrl.path]
            )
        } catch {
            postFailure(error.localizedDescription)
        }
    }

    private func postFailure(_ message: String) {
        NotificationCenter.default.post(
            name: proofCameraFailureNotification,
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
