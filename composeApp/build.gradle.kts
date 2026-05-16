import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

val localProperties = Properties().apply {
    val localPropertiesFile = rootProject.file("local.properties")
    if (localPropertiesFile.isFile) {
        localPropertiesFile.inputStream().use(::load)
    }
}

fun secretProperty(name: String): String =
    providers.gradleProperty(name).orNull
        ?: providers.environmentVariable(name).orNull
        ?: localProperties.getProperty(name).orEmpty()

fun String.asBuildConfigString(): String =
    "\"${replace("\\", "\\\\").replace("\"", "\\\"")}\""

val revenueCatDebugApiKey = secretProperty("REVENUECAT_TEST_API_KEY")
    .ifBlank { "test_RwBAwEyvcmQvyxwDLqrNbIbcrcu" }
val revenueCatAndroidApiKey = secretProperty("REVENUECAT_ANDROID_API_KEY")

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.hotswan.compiler)
    alias(libs.plugins.kotlinSerialization)
}

if (file("google-services.json").exists()) {
    apply(plugin = "com.google.gms.google-services")
}

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }
    
    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
        }
    }
    
    sourceSets {
        named { it.lowercase().startsWith("ios") }.configureEach {
            languageSettings {
                optIn("kotlinx.cinterop.ExperimentalForeignApi")
            }
        }
        androidMain.dependencies {
            implementation(project.dependencies.platform("com.google.firebase:firebase-bom:${libs.versions.firebase.bom.get()}"))
            implementation("com.google.firebase:firebase-messaging")
            implementation(compose.preview)
            implementation(libs.androidx.activity.compose)
            implementation(libs.androidx.exifinterface)
            implementation(libs.ktor.client.android)
            implementation(libs.koin.android)
            implementation(libs.koin.androidx.compose)
            implementation(libs.play.services.auth)
        }
        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
        }
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.material.icons.extended.v173)
            implementation(compose.ui)
            implementation(libs.coil.compose)
            implementation(libs.coil.network.ktor3)
            implementation(libs.compose.components.resources)
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)
            implementation(libs.firebase.app)
            implementation(libs.firebase.auth)
            implementation(libs.firebase.firestore)
            implementation(libs.firebase.storage)
            implementation(libs.firebase.functions)
            implementation(libs.navigation.compose)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.datetime)
            implementation(libs.koin.core)
            implementation(libs.koin.compose)
            implementation(libs.koin.compose.viewmodel)
            implementation(libs.grant.core)
            implementation(libs.purchases.core)
            implementation(libs.purchases.result)
            implementation(libs.vico.compose)
            implementation(libs.vico.compose.m3)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}

android {
    namespace = "com.sortoapps.chorepilot"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    buildFeatures {
        buildConfig = true
    }

    defaultConfig {
        applicationId = "com.sortoapps.chorepilot"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 2
        versionName = "1.0"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        getByName("debug") {
            buildConfigField("String", "REVENUECAT_API_KEY", revenueCatDebugApiKey.asBuildConfigString())
        }
        getByName("release") {
            isMinifyEnabled = false
            buildConfigField("String", "REVENUECAT_API_KEY", revenueCatAndroidApiKey.asBuildConfigString())
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

hotSwanCompiler {
    enabled = true
    debugOnly = true
    preview {
        outputDir.set(".hotswan/preview-captures")
        renderDelayMs.set(1500L)
        sdkModeEnabled.set(false)
    }
}

dependencies {
    debugImplementation(compose.uiTooling)
    debugImplementation(libs.hotswan.preview)
}

val verifyReleaseRevenueCatKey by tasks.registering {
    doLast {
        if (revenueCatAndroidApiKey.isBlank() || revenueCatAndroidApiKey.startsWith("test_")) {
            throw GradleException(
                "Android release builds need a production RevenueCat API key. Add REVENUECAT_ANDROID_API_KEY=goog_... to local.properties, Gradle properties, or the CI environment.",
            )
        }
    }
}

tasks.matching { it.name == "preReleaseBuild" }.configureEach {
    dependsOn(verifyReleaseRevenueCatKey)
}

// The iOS app links Firebase through CocoaPods/Xcode. Kotlin/Native's standalone
// iOS test executable does not build those Pod frameworks, so Gradle `check`
// fails at link time before running useful tests. Shared logic is covered by
// Android debug unit tests until the iOS target has a dedicated native test host.
tasks.matching {
    it.name in setOf(
        "iosArm64TestBinaries",
        "iosSimulatorArm64Test",
        "iosSimulatorArm64TestBinaries",
        "linkDebugTestIosArm64",
        "linkDebugTestIosSimulatorArm64",
    )
}.configureEach {
    enabled = false
}
