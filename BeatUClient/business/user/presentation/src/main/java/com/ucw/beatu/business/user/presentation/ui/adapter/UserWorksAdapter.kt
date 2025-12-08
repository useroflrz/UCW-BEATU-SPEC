package com.ucw.beatu.business.user.presentation.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.ucw.beatu.business.user.presentation.R
import com.ucw.beatu.shared.common.util.NumberFormatter
import com.ucw.beatu.shared.designsystem.util.IOSButtonEffect

class UserWorksAdapter(
    private val onVideoClick: (UserWorkUiModel) -> Unit
) : ListAdapter<UserWorkUiModel, UserWorksAdapter.UserWorkViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserWorkViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_user_work, parent, false)
        return UserWorkViewHolder(view)
    }

    override fun onBindViewHolder(holder: UserWorkViewHolder, position: Int) {
        holder.bind(getItem(position), onVideoClick)
    }

    class UserWorkViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val thumbnail: ImageView = itemView.findViewById(R.id.iv_thumbnail)
        private val playCount: TextView = itemView.findViewById(R.id.tv_play_count)

        fun bind(item: UserWorkUiModel, onVideoClick: (UserWorkUiModel) -> Unit) {
            val placeholderRes = R.drawable.ic_avatar_placeholder
            val thumbnailUrl = item.thumbnailUrl
            
            // 处理本地文件路径和网络URL
            when {
                thumbnailUrl.isNullOrBlank() -> {
                    thumbnail.load(placeholderRes)
                }
                thumbnailUrl.startsWith("http://") || thumbnailUrl.startsWith("https://") -> {
                    // 网络URL
                    thumbnail.load(thumbnailUrl) {
                        crossfade(true)
                        placeholder(placeholderRes)
                        error(placeholderRes)
                    }
                }
                else -> {
                    // 本地文件路径
                    thumbnail.load(java.io.File(thumbnailUrl)) {
                        crossfade(true)
                        placeholder(placeholderRes)
                        error(placeholderRes)
                    }
                }
            }
            
            playCount.text = NumberFormatter.formatCount(item.playCount)
            IOSButtonEffect.applyIOSEffect(itemView) {
                onVideoClick(item)
            }
        }
    }

    companion object DiffCallback : DiffUtil.ItemCallback<UserWorkUiModel>() {
        override fun areItemsTheSame(oldItem: UserWorkUiModel, newItem: UserWorkUiModel): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(
            oldItem: UserWorkUiModel,
            newItem: UserWorkUiModel
        ): Boolean {
            return oldItem == newItem
        }
    }
}

data class UserWorkUiModel(
    val id: Long,  // ✅ 修改：从 String 改为 Long
    val thumbnailUrl: String?,
    val playCount: Long,  // ✅ 修改：这是 viewCount（观看数），不是 playCount
    val playUrl: String,
    val title: String
)

