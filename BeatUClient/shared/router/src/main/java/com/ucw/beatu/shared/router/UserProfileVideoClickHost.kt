package com.ucw.beatu.shared.router

import com.ucw.beatu.shared.common.model.VideoItem

/**
 * 用户作品点击回调宿主接口
 *
 * 由承载用户资料页面的父 Fragment（例如 UserProfileDialogFragment）实现，
 * UserProfileFragment 在只读模式下通过该接口把「作品点击」事件回调给父级，
 * 由父级决定如何展示作品列表（例如跳转到 UserWorksViewerFragment）。
 */
interface UserProfileVideoClickHost {
    /**
     * 当用户在资料页点击「某个作品」时回调
     *
     * @param userId 当前用户 ID
     * @param authorName 作者名称
     * @param videoItems 该用户的作品列表
     * @param initialIndex 当前点击作品在列表中的下标
     */
    fun onUserWorkClicked(
        userId: String,
        authorName: String,
        videoItems: ArrayList<VideoItem>,
        initialIndex: Int
    )
}


