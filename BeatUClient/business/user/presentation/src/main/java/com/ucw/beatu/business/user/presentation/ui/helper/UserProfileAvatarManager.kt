package com.ucw.beatu.business.user.presentation.ui.helper

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Outline
import android.net.Uri
import android.util.Log
import android.view.View
import android.view.ViewOutlineProvider
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.fragment.app.Fragment
import coil.load
import com.ucw.beatu.business.user.presentation.viewmodel.UserProfileViewModel
import com.ucw.beatu.shared.designsystem.R as DesignSystemR
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

/**
 * 管理用户主页的头像上传相关逻辑
 */
class UserProfileAvatarManager(
    private val fragment: Fragment,
    private val ivAvatar: ImageView,
    private val viewModel: UserProfileViewModel,
    private val pickImageLauncher: ActivityResultLauncher<String>,
    private val getCurrentUserId: () -> String?
) {
    companion object {
        private const val TAG = "UserProfileAvatarManager"
    }

    /**
     * 设置头像圆角裁剪（使用 post 解决宽高=0 的问题）
     */
    fun setupAvatarRoundCorner() {
        ivAvatar.post {
            val size = ivAvatar.width.coerceAtMost(ivAvatar.height)
            ivAvatar.outlineProvider = object : ViewOutlineProvider() {
                override fun getOutline(v: View, outline: Outline) {
                    outline.setOval(0, 0, size, size)
                }
            }
            ivAvatar.clipToOutline = true
        }
    }

    /**
     * 设置头像点击上传功能
     */
    fun setupAvatarUpload() {
        com.ucw.beatu.shared.designsystem.util.IOSButtonEffect.applyIOSEffect(ivAvatar) {
            openImagePicker()
        }
    }

    /**
     * 打开图片选择器
     */
    private fun openImagePicker() {
        try {
            pickImageLauncher.launch("image/*")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open image picker", e)
            Toast.makeText(fragment.requireContext(), "无法打开图片选择器: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * 处理图片选择结果
     */
    fun handleImageSelection(uri: Uri) {
        try {
            Log.d(TAG, "Handling image selection: $uri")

            // 检查用户ID是否存在
            val currentUserId = getCurrentUserId()
            if (currentUserId == null) {
                Log.e(TAG, "Cannot update avatar: currentUserId is null")
                Toast.makeText(fragment.requireContext(), "无法更新头像：用户信息未加载", Toast.LENGTH_SHORT).show()
                return
            }

            // 读取图片
            val inputStream = fragment.requireContext().contentResolver.openInputStream(uri)
                ?: run {
                    Log.e(TAG, "Failed to open input stream for URI: $uri")
                    Toast.makeText(fragment.requireContext(), "无法读取图片", Toast.LENGTH_SHORT).show()
                    return
                }

            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream.close()

            if (bitmap == null) {
                Log.e(TAG, "Failed to decode bitmap from URI: $uri")
                Toast.makeText(fragment.requireContext(), "无法解析图片", Toast.LENGTH_SHORT).show()
                return
            }

            Log.d(TAG, "Bitmap decoded successfully: ${bitmap.width}x${bitmap.height}")

            // 保存到本地文件
            val avatarFile = saveAvatarToLocal(bitmap, currentUserId)
            if (avatarFile != null) {
                Log.d(TAG, "Avatar saved to: ${avatarFile.absolutePath}")

                // 更新数据库
                viewModel.updateAvatar(currentUserId, avatarFile.absolutePath)

                // 更新 UI
                ivAvatar.setImageBitmap(bitmap)

                Toast.makeText(fragment.requireContext(), "头像更新成功", Toast.LENGTH_SHORT).show()
            } else {
                Log.e(TAG, "Failed to save avatar to local file")
                Toast.makeText(fragment.requireContext(), "保存头像失败", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error handling image selection", e)
            Toast.makeText(fragment.requireContext(), "处理图片失败: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * 保存头像到本地文件
     */
    private fun saveAvatarToLocal(bitmap: Bitmap, userId: String): File? {
        return try {
            // 创建头像目录
            val avatarDir = File(fragment.requireContext().filesDir, "avatars")
            if (!avatarDir.exists()) {
                val created = avatarDir.mkdirs()
                Log.d(TAG, "Created avatar directory: $created, path: ${avatarDir.absolutePath}")
            }

            // 创建头像文件
            val avatarFile = File(avatarDir, "avatar_${userId}.jpg")

            // 压缩并保存
            FileOutputStream(avatarFile).use { outputStream ->
                val compressed = bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
                if (!compressed) {
                    Log.e(TAG, "Failed to compress bitmap")
                    return null
                }
                outputStream.flush()
            }

            // 验证文件是否创建成功
            if (!avatarFile.exists() || avatarFile.length() == 0L) {
                Log.e(TAG, "Avatar file not created or empty: ${avatarFile.absolutePath}")
                return null
            }

            Log.d(TAG, "Avatar file saved successfully: ${avatarFile.absolutePath}, size: ${avatarFile.length()} bytes")
            avatarFile
        } catch (e: IOException) {
            Log.e(TAG, "IOException while saving avatar", e)
            null
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error while saving avatar", e)
            null
        }
    }

    /**
     * 加载头像（支持本地文件和网络URL）
     */
    fun loadAvatar(avatarUrl: String?) {
        if (avatarUrl.isNullOrBlank()) {
            // 如果头像URL为空，使用占位图
            ivAvatar.setImageResource(DesignSystemR.drawable.ic_avatar_placeholder)
            return
        }

        try {
            // 判断是本地文件路径还是网络URL
            if (avatarUrl.startsWith("http://") || avatarUrl.startsWith("https://")) {
                // 网络URL：使用Coil加载
                ivAvatar.load(avatarUrl) {
                    crossfade(true)
                    placeholder(DesignSystemR.drawable.ic_avatar_placeholder)
                    error(DesignSystemR.drawable.ic_avatar_placeholder)
                }
            } else {
                // 本地文件路径：检查文件是否存在
                val file = File(avatarUrl)
                if (file.exists()) {
                    val bitmap = BitmapFactory.decodeFile(avatarUrl)
                    if (bitmap != null) {
                        ivAvatar.setImageBitmap(bitmap)
                    } else {
                        ivAvatar.setImageResource(DesignSystemR.drawable.ic_avatar_placeholder)
                    }
                } else {
                    ivAvatar.setImageResource(DesignSystemR.drawable.ic_avatar_placeholder)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load avatar: $avatarUrl", e)
            ivAvatar.setImageResource(DesignSystemR.drawable.ic_avatar_placeholder)
        }
    }
}

