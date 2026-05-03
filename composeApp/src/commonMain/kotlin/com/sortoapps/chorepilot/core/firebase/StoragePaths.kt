package com.sortoapps.chorepilot.core.firebase

internal object StoragePaths {
    private const val Images = "images"

    fun proofImage(
        householdId: String,
        choreId: String,
        userId: String,
        proofId: String,
        fileExtension: String,
    ): String = buildString {
        append(FirestorePaths.Households)
        append('/')
        append(householdId)
        append('/')
        append(Images)
        append('/')
        append(FirestorePaths.Chores)
        append('/')
        append(choreId)
        append('/')
        append(FirestorePaths.Proofs)
        append('/')
        append(userId)
        append('/')
        append(proofId)
        append('.')
        append(fileExtension.trimStart('.'))
    }

    fun discoveryImage(
        householdId: String,
        userId: String,
        discoverySessionId: String,
        fileExtension: String,
    ): String = buildString {
        append(FirestorePaths.Households)
        append('/')
        append(householdId)
        append('/')
        append(Images)
        append('/')
        append("discovery")
        append('/')
        append(userId)
        append('/')
        append(discoverySessionId)
        append('.')
        append(fileExtension.trimStart('.'))
    }
}
