package com.ucw.beatu.business.videofeed.presentation.ui

import android.app.Dialog
import android.os.Bundle
import android.content.res.Configuration
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import androidx.fragment.app.DialogFragment
import com.ucw.beatu.business.videofeed.presentation.R

class VideoShareDialogFragment : DialogFragment() {

    interface ShareActionListener {
        fun onSharePoster()
        fun onShareLink()
    }

    companion object {
        private const val ARG_VIDEO_ID = "video_id"
        private const val ARG_TITLE = "title"
        private const val ARG_PLAY_URL = "play_url"

        fun newInstance(videoId: String, title: String, playUrl: String): VideoShareDialogFragment {
            return VideoShareDialogFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_VIDEO_ID, videoId)
                    putString(ARG_TITLE, title)
                    putString(ARG_PLAY_URL, playUrl)
                }
            }
        }
    }

    var shareActionListener: ShareActionListener? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = Dialog(requireContext())
        dialog.setContentView(R.layout.dialog_video_share_options)

        val window = dialog.window
        val isLandscape =
            resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

        if (isLandscape) {
            // 横屏：右侧贴边的竖向条形弹窗，顶到底
            window?.setLayout(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.MATCH_PARENT
            )
            window?.setGravity(Gravity.END)
            window?.addFlags(
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
            )
            window?.decorView?.setPadding(0, 0, 0, 0)
        } else {
            // 竖屏：底部弹出
            window?.setLayout(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT
            )
            window?.setGravity(Gravity.BOTTOM)
        }
        window?.setBackgroundDrawableResource(android.R.color.transparent)

        dialog.findViewById<View>(R.id.btn_share_poster)?.setOnClickListener {
            shareActionListener?.onSharePoster()
            dismiss()
        }

        dialog.findViewById<View>(R.id.btn_share_link)?.setOnClickListener {
            shareActionListener?.onShareLink()
            dismiss()
        }

        return dialog
    }
}

