package com.ucw.beatu.business.ai.presentation.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import com.ucw.beatu.business.ai.presentation.R

/**
 * AI 评论助手弹层（占位）
 * - 半屏弹出
 * - 支持输入 @元宝 触发 AI 问答
 */
class AiCommentDialogFragment : DialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_ai_comment_dialog, container, false)
    }

    override fun onStart() {
        super.onStart()
        // 设置为半屏：宽度 100%，高度自适应
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }
}