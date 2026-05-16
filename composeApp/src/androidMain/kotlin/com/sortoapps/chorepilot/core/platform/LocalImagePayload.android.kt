package com.sortoapps.chorepilot.core.platform

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import dev.gitlive.firebase.storage.Data
import java.io.ByteArrayOutputStream
import java.io.File

private const val MaxUploadEdgePx = 1280
private const val UploadJpegQuality = 34

actual fun localImagePathToBase64(
    localPath: String,
): String = Base64.encodeToString(
    optimizedImageBytes(localPath),
    Base64.NO_WRAP,
)

actual fun localImagePathToFirebaseStorageData(
    localPath: String,
): Data = Data(optimizedImageBytes(localPath))

private fun optimizedImageBytes(
    localPath: String,
): ByteArray {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(localPath, bounds)
    val sampleSize = calculateInSampleSize(bounds.outWidth, bounds.outHeight)
    val bitmap = BitmapFactory.decodeFile(
        localPath,
        BitmapFactory.Options().apply { inSampleSize = sampleSize },
    ) ?: return File(localPath).readBytes()
    val scaled = bitmap.scaleDownIfNeeded()
    return ByteArrayOutputStream().use { output ->
        scaled.compress(Bitmap.CompressFormat.JPEG, UploadJpegQuality, output)
        if (scaled != bitmap) scaled.recycle()
        bitmap.recycle()
        output.toByteArray()
    }
}

private fun Bitmap.scaleDownIfNeeded(): Bitmap {
    val longestEdge = maxOf(width, height)
    if (longestEdge <= MaxUploadEdgePx) return this
    val scale = MaxUploadEdgePx.toFloat() / longestEdge.toFloat()
    val targetWidth = (width * scale).toInt().coerceAtLeast(1)
    val targetHeight = (height * scale).toInt().coerceAtLeast(1)
    return Bitmap.createScaledBitmap(this, targetWidth, targetHeight, true)
}

private fun calculateInSampleSize(
    width: Int,
    height: Int,
): Int {
    var sampleSize = 1
    var halfWidth = width / 2
    var halfHeight = height / 2
    while (halfWidth / sampleSize >= MaxUploadEdgePx && halfHeight / sampleSize >= MaxUploadEdgePx) {
        sampleSize *= 2
    }
    return sampleSize.coerceAtLeast(1)
}
