This is a Kotlin Multiplatform project targeting Android, iOS.

* [/composeApp](./composeApp/src) is for code that will be shared across your Compose Multiplatform applications.
  It contains several subfolders:
  - [commonMain](./composeApp/src/commonMain/kotlin) is for code that’s common for all targets.
  - Other folders are for Kotlin code that will be compiled for only the platform indicated in the folder name.
    For example, if you want to use Apple’s CoreCrypto for the iOS part of your Kotlin app,
    the [iosMain](./composeApp/src/iosMain/kotlin) folder would be the right place for such calls.
    Similarly, if you want to edit the Desktop (JVM) specific part, the [jvmMain](./composeApp/src/jvmMain/kotlin)
    folder is the appropriate location.

* [/iosApp](./iosApp/iosApp) contains iOS applications. Even if you’re sharing your UI with Compose Multiplatform,
  you need this entry point for your iOS app. This is also where you should add SwiftUI code for your project.

### Build and Run Android Application

To build and run the development version of the Android app, use the run configuration from the run widget
in your IDE’s toolbar or build it directly from the terminal:
- on macOS/Linux
  ```shell
  ./gradlew :composeApp:assembleDebug
  ```
- on Windows
  ```shell
  .\gradlew.bat :composeApp:assembleDebug
  ```

### Build and Run iOS Application

To build and run the development version of the iOS app, use the run configuration from the run widget
in your IDE’s toolbar or open the [/iosApp](./iosApp) directory in Xcode and run it from there.

---

Learn more about [Kotlin Multiplatform](https://www.jetbrains.com/help/kotlin-multiplatform-dev/get-started.html)…

### Firebase Setup For Local Production Builds

ChorePilot now fails closed when Firebase is missing or broken instead of silently dropping into local demo behavior.

Expected local platform config files:

- Android: `composeApp/google-services.json`
- iOS: `iosApp/iosApp/GoogleService-Info.plist`

Those files are expected locally and should not be committed. The repo `.gitignore` includes both paths.

Firebase project setup expected by the app:

- Firebase Authentication with Google sign-in enabled
- Cloud Firestore enabled
- Cloud Storage enabled with a default bucket
- Cloud Functions deployed for `deleteHouseholdCascade`
- Firestore and Storage rules deployed from this repo

What happens when Firebase is misconfigured:

- Android shows a clear setup error if `composeApp/google-services.json` is missing or its generated resources do not initialize Firebase
- iOS shows a clear setup error if `iosApp/iosApp/GoogleService-Info.plist` is missing or Firebase fails to initialize from it
- Sign-in and household creation/join flows stay unavailable until the platform config is fixed

Useful local checks:

```shell
./gradlew :composeApp:assembleDebug
firebase deploy --only firestore:rules,storage
```

