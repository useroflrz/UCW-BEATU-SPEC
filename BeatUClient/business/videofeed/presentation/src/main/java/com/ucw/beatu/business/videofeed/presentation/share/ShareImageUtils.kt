package com.ucw.beatu.business.videofeed.presentation.share

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

/**
 * 将 Bitmap 保存为临时文件并通过系统分享图片。
 *
 * 注意：需要在宿主 app 的 AndroidManifest 中配置 FileProvider，
 * 这里默认使用 authority = "${applicationId}.fileprovider"。
 */
object ShareImageUtils {

    fun shareBitmap(
        context: Context,
        bitmap: Bitmap,
        fileName: String,
        chooserTitle: String
    ) {
        val cacheDir = File(context.cacheDir, "share_images").apply {
            if (!exists()) mkdirs()
        }
        val outFile = File(cacheDir, fileName)
        FileOutputStream(outFile).use { fos ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, fos)
        }

        // 与 AndroidManifest 中 provider 的 authorities 保持一致
        val authority = "com.ucw.beatu.fileprovider"
        val uri: Uri = FileProvider.getUriForFile(context, authority, outFile)

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "image/*"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        context.startActivity(
            Intent.createChooser(shareIntent, chooserTitle)
        )
    }
}


