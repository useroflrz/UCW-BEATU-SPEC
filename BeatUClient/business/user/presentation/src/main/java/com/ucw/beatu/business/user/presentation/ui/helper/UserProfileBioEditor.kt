package com.ucw.beatu.business.user.presentation.ui.helper

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.widget.EditText
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import com.ucw.beatu.business.user.presentation.viewmodel.UserProfileViewModel

/**
 * 管理用户主页的名言编辑对话框（iOS风格）
 */
class UserProfileBioEditor(
    private val fragment: Fragment,
    private val viewModel: UserProfileViewModel,
    private val getCurrentBio: () -> String,
    private val getCurrentUserId: () -> String?
) {
    /**
     * 显示编辑名言对话框（iOS风格，圆形边框）
     */
    fun showEditBioDialog() {
        val currentBio = getCurrentBio()
        val context = fragment.requireContext()

        // 创建iOS风格的输入框（白色圆边框，完整包围所有文字）
        val input = EditText(context).apply {
            setText(currentBio)
            setSelection(currentBio.length)
            hint = "请输入一句话介绍自己"
            textSize = 17f  // iOS标准字体大小
            minLines = 2
            maxLines = 4
            val padding16 = 16f.dpToPx(context).toInt()
            val padding12 = 12f.dpToPx(context).toInt()
            // iOS风格的内边距，确保文字在边框内显示
            setPadding(padding16, padding12, padding16, padding12)
            setHintTextColor(0x99000000.toInt()) // iOS风格的提示文字颜色
            setTextColor(0xFF000000.toInt()) // 黑色文字
            // iOS风格的背景，圆角矩形，浅灰色边框
            background = createIOSEditBackground(context)
            // 确保输入框宽度填满容器
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        // 创建容器，添加适当的内边距
        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            val padding16 = 16f.dpToPx(context).toInt()
            setPadding(padding16, 0, padding16, 0)
            // 设置容器背景为透明
            setBackgroundColor(0x00000000.toInt())
            addView(input)

            // 添加容器底部内边距
            val params = layoutParams as? LinearLayout.LayoutParams
            params?.bottomMargin = 24f.dpToPx(context).toInt()
        }

        // iOS风格的对话框
        val dialog = android.app.AlertDialog.Builder(context, android.R.style.Theme_Material_Light_Dialog_Alert)
            .setTitle("编辑简介")
            .setView(container)
            .setPositiveButton("保存", null) // 先设为null，后面自定义
            .setNegativeButton("取消", null)
            .create()

        // 设置对话框背景为iOS风格（圆角、浅色）
        dialog.window?.apply {
            // 设置窗口背景为透明，让我们自定义的圆角生效
            setBackgroundDrawableResource(android.R.color.transparent)
            // 设置动画为iOS风格
//            setWindowAnimations(com.ucw.beatu.R.style.iOSDialogAnimation)
        }

        // 自定义按钮样式
        dialog.setOnShowListener {
            val positiveButton = dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE)
            val negativeButton = dialog.getButton(android.app.AlertDialog.BUTTON_NEGATIVE)

            // iOS风格的按钮样式
            positiveButton.setTextColor(0xFF007AFF.toInt()) // iOS系统蓝色
            positiveButton.textSize = 17f
            positiveButton.isAllCaps = false // 禁用大写

            negativeButton.setTextColor(0xFF007AFF.toInt())
            negativeButton.textSize = 17f
            negativeButton.isAllCaps = false

            // iOS风格的按钮点击效果
            positiveButton.setOnClickListener {
                val newBio = input.text.toString().trim()
                val currentUserId = getCurrentUserId() ?: return@setOnClickListener
                viewModel.updateBio(currentUserId, newBio.ifEmpty { null })
                dialog.dismiss()
            }

            negativeButton.setOnClickListener {
                dialog.dismiss()
            }

            // 确保对话框有适当的圆角（iOS风格）
            val dialogWindow = dialog.window
            dialogWindow?.setBackgroundDrawable(createIOSDialogBackground(context))
        }

        dialog.show()
    }

    /**
     * 创建iOS风格的编辑框背景（白色圆边框，完整包围所有内容）
     */
    private fun createIOSEditBackground(context: Context): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = 8f.dpToPx(context) // iOS标准圆角
            setColor(0xFFFFFFFF.toInt()) // 白色背景
            // iOS风格的边框：更细、更浅的灰色
            setStroke(1f.dpToPx(context).toInt(), 0xFFC7C7CC.toInt()) // iOS标准边框颜色

            // 设置内边距确保边框内的内容区域
            val inset = 1f.dpToPx(context).toInt()
            setBounds(inset, inset, inset, inset)
        }
    }

    /**
     * 创建iOS风格对话框背景
     */
    private fun createIOSDialogBackground(context: Context): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = 14f.dpToPx(context) // iOS对话框圆角较大
            setColor(0xFFF2F2F7.toInt()) // iOS系统背景色
        }
    }

    private fun Float.dpToPx(context: Context): Float {
        return this * context.resources.displayMetrics.density
    }
}