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

class UserWorksAdapter :
    ListAdapter<UserWorkUiModel, UserWorksAdapter.UserWorkViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserWorkViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_user_work, parent, false)
        return UserWorkViewHolder(view)
    }

    override fun onBindViewHolder(holder: UserWorkViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class UserWorkViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val thumbnail: ImageView = itemView.findViewById(R.id.iv_thumbnail)
        private val playCount: TextView = itemView.findViewById(R.id.tv_play_count)

        fun bind(item: UserWorkUiModel) {
            val placeholderRes = R.drawable.ic_avatar_placeholder
            thumbnail.load(item.thumbnailUrl ?: placeholderRes) {
                crossfade(true)
                placeholder(placeholderRes)
                error(placeholderRes)
            }
            playCount.text = formatPlayCount(item.playCount)
        }

        private fun formatPlayCount(count: Long): String {
            return when {
                count >= 100000000 -> String.format("%.1f亿", count / 100000000.0)
                count >= 10000 -> String.format("%.1f万", count / 10000.0)
                count >= 1000 -> String.format("%.1f千", count / 1000.0)
                else -> count.toString()
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
    val id: String,
    val thumbnailUrl: String?,
    val playCount: Long
)

