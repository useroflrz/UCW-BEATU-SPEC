package com.ucw.beatu.business.videofeed.presentation.ui

import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import com.ucw.beatu.business.videofeed.presentation.R
import com.ucw.beatu.shared.router.RouterRegistry

/**
 * 用户信息弹窗（类似评论弹窗）
 * - 从底部弹出，占据下半部分
 * - 视频会缩小到上半部分（由父 Fragment 控制）
 */
class UserProfileDialogFragment : DialogFragment() {

    private var userId: String? = null
    private var authorName: String? = null
    private var onDismissListener: (() -> Unit)? = null
    private var onVideoClickListener: ((String, String, ArrayList<com.ucw.beatu.shared.common.model.VideoItem>, Int) -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 设置 Dialog 样式为底部弹出（类似评论弹窗）
        setStyle(STYLE_NORMAL, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
        userId = arguments?.getString(ARG_USER_ID)
        authorName = arguments?.getString(ARG_AUTHOR_NAME)
    }
    
    override fun onCreateDialog(savedInstanceState: Bundle?): android.app.Dialog {
        return super.onCreateDialog(savedInstanceState).apply {
            // 允许点击外部区域关闭Dialog（点击上半部分视频区域会关闭）
            // 但是点击Dialog内部区域不会关闭
            setCanceledOnTouchOutside(true)
            setCancelable(true)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_user_profile_dialog, container, false)
        // 不设置点击监听器，让子视图（如关注按钮）能够正常接收点击事件
        // Dialog的setCanceledOnTouchOutside(true)已经处理了外部点击关闭的逻辑
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 创建 UserProfileFragment 并添加到容器中
        val userId = this.userId ?: authorName ?: return
        val router = RouterRegistry.getUserProfileRouter()
        if (router != null) {
            val userProfileFragment = router.createUserProfileFragment(userId, authorName ?: "", readOnly = true)
            childFragmentManager.beginTransaction()
                .replace(R.id.user_profile_container, userProfileFragment)
                .commit()
            
            // 设置视频点击回调（通过反射或接口）
            // 由于UserProfileFragment在另一个模块，我们使用延迟查找的方式
            view.post {
                val fragment = childFragmentManager.findFragmentById(R.id.user_profile_container)
                if (fragment != null) {
                    try {
                        // 使用反射设置回调
                        val method = fragment.javaClass.getMethod("setOnVideoClickListener", 
                            android.os.Bundle::class.java, 
                            String::class.java, 
                            java.util.ArrayList::class.java, 
                            Int::class.java)
                        // 创建一个Bundle来传递回调信息
                        val callbackBundle = android.os.Bundle().apply {
                            putString("callback_type", "video_click")
                        }
                        // 这里我们需要一个更好的方式来传递回调
                        // 暂时先不设置，让UserProfileFragment直接调用parentFragment的方法
                    } catch (e: Exception) {
                        // 如果方法不存在，忽略
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.let { window ->
            // 使用 resources.displayMetrics 获取屏幕尺寸（推荐方式，避免废弃警告）
            val metrics = resources.displayMetrics
            val params: WindowManager.LayoutParams = window.attributes

            val isLandscape =
                resources.configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE

            if (isLandscape) {
                // 横屏：右侧半屏浮层（和评论弹窗一致）
                params.width = (metrics.widthPixels * 0.4f).toInt()
                params.height = metrics.heightPixels
                params.gravity = Gravity.END or Gravity.TOP
            } else {
                // 竖屏：底部半屏（和评论弹窗一致，55%高度）
                params.width = ViewGroup.LayoutParams.MATCH_PARENT
                params.height = (metrics.heightPixels * 0.55f).toInt()
                params.gravity = Gravity.BOTTOM
            }

            window.attributes = params
            window.setBackgroundDrawableResource(android.R.color.black)
            window.decorView.setPadding(0, 0, 0, 0)
            
            // 设置从底部弹出的动画（仅在竖屏时）
            if (!isLandscape) {
                window.setWindowAnimations(R.style.DialogAnimationSlideFromBottom)
            }
            
            if (isLandscape) {
                // 允许内容布局到状态栏区域，进一步消除顶端空隙
                window.addFlags(
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                        WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                )
            }
        }
    }

    override fun onDismiss(dialog: android.content.DialogInterface) {
        super.onDismiss(dialog)
        onDismissListener?.invoke()
    }

    fun setOnDismissListener(listener: () -> Unit) {
        onDismissListener = listener
    }
    
    fun setOnVideoClickListener(listener: (String, String, ArrayList<com.ucw.beatu.shared.common.model.VideoItem>, Int) -> Unit) {
        onVideoClickListener = listener
    }
    
    fun notifyVideoClick(userId: String, authorName: String, videoItems: ArrayList<com.ucw.beatu.shared.common.model.VideoItem>, initialIndex: Int) {
        onVideoClickListener?.invoke(userId, authorName, videoItems, initialIndex)
    }

    companion object {
        private const val ARG_USER_ID = "user_id"
        private const val ARG_AUTHOR_NAME = "author_name"

        fun newInstance(userId: String, authorName: String): UserProfileDialogFragment {
            return UserProfileDialogFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_USER_ID, userId)
                    putString(ARG_AUTHOR_NAME, authorName)
                }
            }
        }
    }
}

