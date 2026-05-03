package com.sortoapps.chorepilot.features.chorecapture.presentation.platform

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import androidx.exifinterface.media.ExifInterface
import com.sortoapps.chorepilot.features.chores.domain.ProofUploadPayload
import java.io.File
import java.io.FileOutputStream

@Composable
actual fun rememberProofCameraHandler(
    onSuccess: (ProofUploadPayload) -> Unit,
    onFailure: (String) -> Unit,
): ProofCameraHandler {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    var pendingCapture by remember { mutableStateOf<PendingCapture?>(null) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture(),
    ) { success ->
        val capture = pendingCapture
        pendingCapture = null
        if (!success || capture == null) {
            onFailure("We couldn't capture that photo. Please try again.")
            return@rememberLauncherForActivityResult
        }
        runCatching {
            val normalizedFile = capture.file.takeIf { it.exists() }?.let(::normalizeCaptureFile)
                ?: error("The captured photo couldn't be read.")
            ProofUploadPayload(
                localPath = normalizedFile.absolutePath,
                previewUri = "file://${normalizedFile.absolutePath}",
                mimeType = "image/jpeg",
                fileExtension = "jpg",
            )
        }.onSuccess(onSuccess)
            .onFailure { error ->
                onFailure(error.message ?: "We couldn't prepare that photo.")
            }
    }

    return remember(activity, launcher) {
        ProofCameraHandler(
            launch = launch@{
                val resolvedActivity = activity
                    ?: run {
                        onFailure("Camera capture needs an active Android screen.")
                        return@launch
                    }
                val uri = runCatching { resolvedActivity.createProofCaptureUri() }
                    .getOrElse { error ->
                        onFailure(error.message ?: "We couldn't prepare the camera.")
                        return@launch
                    }
                pendingCapture = uri
                launcher.launch(uri.uri)
            },
        )
    }
}

private data class PendingCapture(
    val file: File,
    val uri: Uri,
)

private fun Activity.createProofCaptureUri(): PendingCapture {
    val baseDir = externalCacheDir ?: cacheDir
    val captureDir = File(baseDir, "proof-capture").apply { mkdirs() }
    val captureFile = File.createTempFile("proof-", ".jpg", captureDir)
    return PendingCapture(
        file = captureFile,
        uri = FileProvider.getUriForFile(
            this,
            "$packageName.fileprovider",
            captureFile,
        ),
    )
}

private fun normalizeCaptureFile(file: File): File {
    val exif = ExifInterface(file)
    val orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
    if (orientation == ExifInterface.ORIENTATION_NORMAL || orientation == ExifInterface.ORIENTATION_UNDEFINED) {
        return file
    }

    val source = BitmapFactory.decodeFile(file.absolutePath) ?: return file
    val matrix = Matrix().apply {
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> preScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> preScale(1f, -1f)
            ExifInterface.ORIENTATION_TRANSPOSE -> {
                preScale(-1f, 1f)
                postRotate(270f)
            }
            ExifInterface.ORIENTATION_TRANSVERSE -> {
                preScale(-1f, 1f)
                postRotate(90f)
            }
        }
    }
    val rotated = Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
    FileOutputStream(file).use { output ->
        rotated.compress(Bitmap.CompressFormat.JPEG, 92, output)
    }
    if (rotated != source) rotated.recycle()
    source.recycle()
    return file
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
