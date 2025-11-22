package com.ucw.beatu.business.user.presentation.ui

import android.graphics.Outline
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.widget.ImageView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ucw.beatu.business.user.presentation.R

/**
 * 用户主页Fragment
 * 显示用户头像、昵称、作品列表等信息
 */
class UserProfileFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: UserWorksAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_user_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 设置头像圆角裁剪
        setupAvatarRoundCorner(view)

        // 初始化作品列表
        initWorksList(view)

        // TODO: 加载用户真实数据
        loadUserData()
    }

    /**
     * 设置头像圆角裁剪（使用 post 解决宽高=0 的问题）
     */
    private fun setupAvatarRoundCorner(view: View) {
        val avatarImageView = view.findViewById<ImageView>(R.id.iv_avatar)

        avatarImageView.post {
            val size = avatarImageView.width.coerceAtMost(avatarImageView.height)
            avatarImageView.outlineProvider = object : ViewOutlineProvider() {
                override fun getOutline(v: View, outline: Outline) {
                    outline.setOval(0, 0, size, size)
                }
            }
            avatarImageView.clipToOutline = true
        }
    }

    /**
     * 初始化作品列表
     */
    private fun initWorksList(view: View) {
        recyclerView = view.findViewById(R.id.rv_works)

        recyclerView.layoutManager = GridLayoutManager(context, 3)

        // 🚀 性能优化：关闭 nested scroll，避免卡顿
        recyclerView.isNestedScrollingEnabled = false

        adapter = UserWorksAdapter(getMockWorksData())
        recyclerView.adapter = adapter
    }

    /**
     * 加载用户数据（假数据）
     */
    private fun loadUserData() {
        // TODO: 从 ViewModel 或 Repository 加载真实数据
    }

    /**
     * 获取假数据
     */
    private fun getMockWorksData(): List<WorkItem> {
        return List(20) { index ->
            WorkItem(
                title = "作品$index",
                thumbnailUrl = "https://picsum.photos/300/300?random=$index"
            )
        }
    }

    /**
     * 数据模型
     */
    data class WorkItem(
        val title: String,
        val thumbnailUrl: String
    )

    /**
     * 作品列表 Adapter
     */
    private class UserWorksAdapter(
        private val works: List<WorkItem>
    ) : RecyclerView.Adapter<UserWorksAdapter.WorkViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WorkViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_user_work, parent, false)
            return WorkViewHolder(view)
        }

        override fun onBindViewHolder(holder: WorkViewHolder, position: Int) {
            holder.bind(works[position])
        }

        override fun getItemCount(): Int = works.size
        //TODO 记载缩缩略图
        class WorkViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

//            private val thumbnail = itemView.findViewById<ImageView>(R.id.iv_work_thumbnail)

            fun bind(work: WorkItem) {
                // 使用 Glide 加载缩略图
//                Glide.with(itemView.context)
//                    .load(work.thumbnailUrl)
//                    .centerCrop()
//                    .into(thumbnail)
            }
        }
    }
}
