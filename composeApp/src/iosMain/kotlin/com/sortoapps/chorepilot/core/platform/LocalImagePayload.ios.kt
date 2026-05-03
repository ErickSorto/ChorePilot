package com.sortoapps.chorepilot.core.platform

import dev.gitlive.firebase.storage.Data
import kotlinx.cinterop.useContents
import platform.CoreGraphics.CGRectMake
import platform.CoreGraphics.CGSizeMake
import platform.Foundation.NSData
import platform.Foundation.base64EncodedStringWithOptions
import platform.Foundation.dataWithContentsOfFile
import platform.UIKit.UIGraphicsBeginImageContextWithOptions
import platform.UIKit.UIGraphicsEndImageContext
import platform.UIKit.UIGraphicsGetImageFromCurrentImageContext
import platform.UIKit.UIImage
import platform.UIKit.UIImageJPEGRepresentation

private const val MaxUploadEdgePx = 1280.0
private const val UploadJpegQuality = 0.28

actual fun localImagePathToBase64(
    localPath: String,
): String = requireNotNull(NSData.dataWithContentsOfFile(localPath)) {
    "Could not read local image at $localPath"
}.let {
    optimizedImageData(localPath, it)
}.base64EncodedStringWithOptions(0u)

actual fun localImagePathToFirebaseStorageData(
    localPath: String,
): Data = Data(
    optimizedImageData(
        localPath,
        requireNotNull(NSData.dataWithContentsOfFile(localPath)) {
            "Could not read local image at $localPath"
        },
    ),
)

private fun optimizedImageData(
    localPath: String,
    original: NSData,
): NSData {
    val image = UIImage.imageWithContentsOfFile(localPath)
        ?: return original
    val (width, height) = image.size.useContents { width to height }
    val longestEdge = maxOf(width, height)
    val scaledImage = if (longestEdge > MaxUploadEdgePx) {
        val scale = MaxUploadEdgePx / longestEdge
        val targetWidth = width * scale
        val targetHeight = height * scale
        UIGraphicsBeginImageContextWithOptions(
            size = CGSizeMake(targetWidth, targetHeight),
            opaque = false,
            scale = 1.0,
        )
        image.drawInRect(CGRectMake(0.0, 0.0, targetWidth, targetHeight))
        val resized = UIGraphicsGetImageFromCurrentImageContext() ?: image
        UIGraphicsEndImageContext()
        resized
    } else {
        image
    }
    return UIImageJPEGRepresentation(scaledImage, UploadJpegQuality)
        ?: original
}
