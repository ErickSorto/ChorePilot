package com.sortoapps.chorepilot.core.platform

import dev.gitlive.firebase.storage.Data

expect fun localImagePathToBase64(
    localPath: String,
): String

expect fun localImagePathToFirebaseStorageData(
    localPath: String,
): Data
