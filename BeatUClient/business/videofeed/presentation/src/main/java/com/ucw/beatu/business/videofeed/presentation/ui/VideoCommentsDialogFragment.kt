package com.ucw.beatu.business.videofeed.presentation.ui

import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Log
import android.view.*
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.ucw.beatu.business.videofeed.domain.model.Comment
import com.ucw.beatu.business.videofeed.domain.usecase.GetCommentsUseCase
import com.ucw.beatu.business.videofeed.domain.usecase.PostCommentUseCase
import com.ucw.beatu.business.videofeed.presentation.R
import com.ucw.beatu.shared.common.result.AppResult
import com.ucw.beatu.shared.common.util.TimeFormatter
import com.ucw.beatu.shared.designsystem.R as DesignSystemR
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 通用评论弹层：
 * - 竖屏：底部半屏
 * - 横屏：右侧半屏（用于 Landscape）
 *
 * 使用真实的后端 API 获取和发布评论
 */
@AndroidEntryPoint
class VideoCommentsDialogFragment : DialogFragment() {

    @Inject
    lateinit var getCommentsUseCase: GetCommentsUseCase

    @Inject
    lateinit var postCommentUseCase: PostCommentUseCase

    private val videoId: String? get() = arguments?.getString(ARG_VIDEO_ID)
    private val initialCommentCount: Int get() = arguments?.getInt(ARG_COMMENT_COUNT) ?: 0

    private var commentsRecyclerView: RecyclerView? = null
    private var inputEditText: EditText? = null
    private var sendButton: TextView? = null
    private var closeButton: ImageView? = null
    private var commentTitleView: TextView? = null
    private var commentPanel: View? = null

    private var adapter: CommentsAdapter? = null
    private var isPosting = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_video_comments_dialog, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        commentPanel = view.findViewById(R.id.layout_comment_panel)
        commentsRecyclerView = view.findViewById(R.id.rv_comments)
        inputEditText = view.findViewById(R.id.et_comment)
        sendButton = view.findViewById(R.id.btn_send)
        closeButton = view.findViewById(R.id.btn_close)
        commentTitleView = view.findViewById(R.id.tv_comment_title)

        commentsRecyclerView?.layoutManager = LinearLayoutManager(requireContext())
        adapter = CommentsAdapter(mutableListOf())
        commentsRecyclerView?.adapter = adapter

        commentTitleView?.text = "${initialCommentCount}条评论"

        closeButton?.setOnClickListener { dismissAllowingStateLoss() }

        sendButton?.setOnClickListener {
            val content = inputEditText?.text?.toString()?.trim().orEmpty()
            if (content.isNotEmpty() && !isPosting) {
                postComment(content)
            }
        }

        loadComments()
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
        
        // 设置评论面板内容左右间距，对称
        commentPanel?.let { panel ->
            val metrics = DisplayMetrics()
            requireActivity().windowManager.defaultDisplay.getMetrics(metrics)
            val horizontalPadding = (metrics.widthPixels * 0.0125f).toInt() // 2.5% 的间距
            
            panel.setPadding(
                horizontalPadding,
                panel.paddingTop,
                horizontalPadding,
                panel.paddingBottom
            )
        }
    }

    private fun loadComments() {
        val id = videoId ?: return
        lifecycleScope.launch {
            getCommentsUseCase(id, page = 1, limit = 30).collect { result ->
                when (result) {
                    is AppResult.Loading -> {
                        // 加载中状态可以显示加载指示器，这里先不处理
                    }
                    is AppResult.Success -> {
                        adapter?.updateComments(result.data)
                    }
                    is AppResult.Error -> {
                        Log.e(TAG, "Failed to load comments", result.throwable)
                        Toast.makeText(
                            requireContext(),
                            "加载评论失败: ${result.throwable.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }
    }

    private fun postComment(content: String) {
        val id = videoId ?: return
        if (isPosting) return

        isPosting = true
        sendButton?.isEnabled = false

        lifecycleScope.launch {
            when (val result = postCommentUseCase(id, content)) {
                is AppResult.Success -> {
                    // 将新评论插入到列表顶部
                    adapter?.prependComment(result.data)
                    commentsRecyclerView?.scrollToPosition(0)
                    inputEditText?.setText("")
                    
                    // 更新评论数量
                    val newCount = initialCommentCount + 1
                    commentTitleView?.text = "${newCount}条评论"
                }
                is AppResult.Error -> {
                    Log.e(TAG, "Failed to post comment", result.throwable)
                    Toast.makeText(
                        requireContext(),
                        "发布评论失败: ${result.throwable.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                is AppResult.Loading -> {
                    // 不会进入这里
                }
            }
            isPosting = false
            sendButton?.isEnabled = true
        }
    }

    companion object {
        private const val TAG = "VideoCommentsDialog"
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
 * 评论列表 Adapter：使用 Domain Model 的 Comment
 */
private class CommentsAdapter(
    private val items: MutableList<Comment>
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

    fun updateComments(newComments: List<Comment>) {
        items.clear()
        items.addAll(newComments)
        notifyDataSetChanged()
    }

    fun prependComment(comment: Comment) {
        items.add(0, comment)
        notifyItemInserted(0)
    }
}

private class CommentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

    private val avatarView: ImageView = itemView.findViewById(R.id.iv_avatar)
    private val userNameView: TextView = itemView.findViewById(R.id.tv_user_name)
    private val contentView: TextView = itemView.findViewById(R.id.tv_content)
    private val timeLocationView: TextView = itemView.findViewById(R.id.tv_time_location)
    private val likeIconView: ImageView = itemView.findViewById(R.id.iv_like_icon)
    private val likeCountView: TextView = itemView.findViewById(R.id.tv_like_count)

    init {
        // 设置圆形裁剪
        avatarView.apply {
            outlineProvider = android.view.ViewOutlineProvider.BACKGROUND
            clipToOutline = true
        }
    }

    fun bind(comment: Comment) {
        val placeholderRes = DesignSystemR.drawable.ic_avatar_placeholder

        val avatarUrl = comment.authorAvatar
        if (avatarUrl.isNullOrBlank()) {
            // 无头像时使用占位图
            avatarView.setImageResource(placeholderRes)
        } else {
            // 使用 Coil 加载网络头像
            avatarView.load(avatarUrl) {
                crossfade(true)
                placeholder(placeholderRes)
                error(placeholderRes)
            }
        }

        userNameView.text = comment.authorName
        contentView.text = comment.content

        // 使用评论日期格式化工具格式化时间显示
        timeLocationView.text = TimeFormatter.formatCommentDate(comment.createdAt)

        // 显示点赞数
        likeCountView.text = comment.likeCount.toString()
    }
}