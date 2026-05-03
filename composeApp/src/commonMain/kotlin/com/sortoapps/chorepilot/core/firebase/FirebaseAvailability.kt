package com.sortoapps.chorepilot.core.firebase

enum class FirebaseAvailabilityStatus {
    Configured,
    MissingPlatformConfig,
    InitializationFailed,
}

data class FirebaseAvailability(
    val status: FirebaseAvailabilityStatus,
    val message: String? = null,
) {
    val isConfigured: Boolean = status == FirebaseAvailabilityStatus.Configured
}
