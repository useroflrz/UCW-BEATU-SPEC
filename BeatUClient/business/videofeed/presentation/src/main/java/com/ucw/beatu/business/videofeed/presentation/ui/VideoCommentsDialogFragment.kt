package com.ucw.beatu.business.videofeed.presentation.ui

import android.os.Bundle
import android.util.DisplayMetrics
import android.view.*
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ucw.beatu.business.videofeed.presentation.R
import com.ucw.beatu.shared.common.mock.MockComment
import com.ucw.beatu.shared.common.mock.MockComments

/**
 * 通用评论弹层：
 * - 竖屏：底部半屏
 * - 横屏：右侧半屏（用于 Landscape）
 *
 * 当前版本先实现 UI 结构与基本交互，数据源后续接入 GetCommentsUseCase/PostCommentUseCase。
 */
class VideoCommentsDialogFragment : DialogFragment() {

    private val videoId: String? get() = arguments?.getString(ARG_VIDEO_ID)
    private val initialCommentCount: Int get() = arguments?.getInt(ARG_COMMENT_COUNT) ?: 0

    private var commentsRecyclerView: RecyclerView? = null
    private var inputEditText: EditText? = null
    private var sendButton: TextView? = null
    private var closeButton: ImageView? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_video_comments_dialog, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        commentsRecyclerView = view.findViewById(R.id.rv_comments)
        inputEditText = view.findViewById(R.id.et_comment)
        sendButton = view.findViewById(R.id.btn_send)
        closeButton = view.findViewById(R.id.btn_close)

        commentsRecyclerView?.layoutManager = LinearLayoutManager(requireContext())
        val commentList = loadMockComments()
        commentsRecyclerView?.adapter = CommentsAdapter(commentList.toMutableList())

        view.findViewById<TextView>(R.id.tv_comment_title)?.apply {
            text = "评论区 $initialCommentCount"
        }

        closeButton?.setOnClickListener { dismissAllowingStateLoss() }

        sendButton?.setOnClickListener {
            val content = inputEditText?.text?.toString()?.trim().orEmpty()
            if (content.isNotEmpty()) {
                // 先用本地 Mock：把自己的评论插入到列表顶部
                (commentsRecyclerView?.adapter as? CommentsAdapter)?.let { adapter ->
                    val newComment = MockComment(
                        id = "local_${System.currentTimeMillis()}",
                        videoId = videoId.orEmpty(),
                        userName = "我",
                        isAuthor = false,
                        timeDesc = "刚刚",
                        location = null,
                        content = content,
                        likeCount = 0
                    )
                    adapter.prependComment(newComment)
                    commentsRecyclerView?.scrollToPosition(0)
                }
                inputEditText?.setText("")
            }
        }
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.let { window ->
            val metrics = DisplayMetrics()
            requireActivity().windowManager.defaultDisplay.getMetrics(metrics)
            val params: WindowManager.LayoutParams = window.attributes

            val isLandscape =
                resources.configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE

            if (isLandscape) {
                // 横屏：右侧半屏浮层
                params.width = (metrics.widthPixels * 0.4f).toInt()
                // 使用整屏高度，配合 FLAG_LAYOUT_IN_SCREEN 让评论区竖直方向贴满
                params.height = metrics.heightPixels
                params.gravity = Gravity.END or Gravity.TOP
            } else {
                // 竖屏：底部半屏
                params.width = ViewGroup.LayoutParams.MATCH_PARENT
                params.height = (metrics.heightPixels * 0.55f).toInt()
                params.gravity = Gravity.BOTTOM
            }

            // 去掉系统 Dialog 默认的 padding，避免横屏右侧评论区顶部留出一条空白
            window.attributes = params
            window.setBackgroundDrawableResource(android.R.color.transparent)
            window.decorView.setPadding(0, 0, 0, 0)
            if (isLandscape) {
                // 允许内容布局到状态栏区域，进一步消除顶端空隙
                window.addFlags(
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                        WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                )
            }
        }
    }

    private fun loadMockComments(): List<MockComment> {
        val id = videoId ?: "unknown_video"
        return MockComments.getCommentsForVideo(id, count = 30)
    }

    companion object {
        private const val ARG_VIDEO_ID = "arg_video_id"
        private const val ARG_COMMENT_COUNT = "arg_comment_count"

        fun newInstance(videoId: String, commentCount: Int): VideoCommentsDialogFragment {
            return VideoCommentsDialogFragment().apply {
                arguments = bundleOf(
                    ARG_VIDEO_ID to videoId,
                    ARG_COMMENT_COUNT to commentCount
                )
            }
        }
    }
}

/**
 * 评论列表 Adapter：目前使用 MockComments 提供的数据。
 */
private class CommentsAdapter(
    private val items: MutableList<MockComment>
) : RecyclerView.Adapter<CommentViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommentViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_video_comment, parent, false)
        return CommentViewHolder(itemView)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: CommentViewHolder, position: Int) {
        holder.bind(items[position])
    }

    fun prependComment(comment: MockComment) {
        items.add(0, comment)
        notifyItemInserted(0)
    }
}

private class CommentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

    private val avatarView: ImageView = itemView.findViewById(R.id.iv_avatar)
    private val userNameView: TextView = itemView.findViewById(R.id.tv_user_name)
    private val authorTagView: TextView = itemView.findViewById(R.id.tv_author_tag)
    private val contentView: TextView = itemView.findViewById(R.id.tv_content)
    private val timeLocationView: TextView = itemView.findViewById(R.id.tv_time_location)
    private val replyView: TextView = itemView.findViewById(R.id.tv_reply)
    private val likeCountView: TextView = itemView.findViewById(R.id.tv_like_count)

    fun bind(comment: MockComment) {
        // 头像目前用统一占位图 + 圆形背景
        avatarView.setImageResource(R.drawable.ic_avatar_placeholder)

        userNameView.text = comment.userName
        authorTagView.visibility = if (comment.isAuthor) View.VISIBLE else View.GONE

        contentView.text = comment.content

        timeLocationView.text = comment.timeDesc

        // 回复按钮暂时不做交互，只展示文字
        replyView.text = "回复"

        likeCountView.text = comment.likeCount.toString()
    }
}


