package com.ucw.beatu.business.landscape.data.repository

import com.ucw.beatu.business.landscape.domain.model.VideoItem
import com.ucw.beatu.business.landscape.domain.model.VideoOrientation
import com.ucw.beatu.business.landscape.domain.repository.LandscapeRepository
import com.ucw.beatu.shared.common.result.AppResult
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow

@Singleton
class LandscapeRepositoryImpl @Inject constructor() : LandscapeRepository {

    private val mockVideos = listOf(
        baseVideo(
            id = "ls_mock_1",
            playUrl = "https://ucw-beatu.oss-cn-shenzhen.aliyuncs.com/Justin%20Bieber%20-%20Beauty%20And%20A%20Beat.mp4",
            title = "横屏极光旅拍",
            author = "KORA Studio"
        ),
        baseVideo(
            id = "ls_mock_2",
            playUrl = "https://ucw-beatu.oss-cn-shenzhen.aliyuncs.com/%E6%B5%8B%E8%AF%95%E8%A7%86%E9%A2%91.mp4",
            title = "机车少年",
            author = "Gear Lab"
        ),
        baseVideo(
            id = "ls_mock_3",
            playUrl = "https://ucw-beatu.oss-cn-shenzhen.aliyuncs.com/%E6%B5%8B%E8%AF%95%E8%A7%86%E9%A2%912.mp4",
            title = "无人机灵感",
            author = "Sky Vision"
        )
    )

    override fun getLandscapeVideos(page: Int, limit: Int): Flow<AppResult<List<VideoItem>>> {
        return flow {
            emit(AppResult.Loading)
            delay(120)
            emit(AppResult.Success(generatePage(page, limit)))
        }.catch { throwable ->
            emit(AppResult.Error(throwable))
        }
    }

    override fun loadMoreLandscapeVideos(page: Int, limit: Int): Flow<AppResult<List<VideoItem>>> {
        return flow {
            emit(AppResult.Loading)
            delay(100)
            emit(AppResult.Success(generatePage(page, limit)))
        }.catch { throwable ->
            emit(AppResult.Error(throwable))
        }
    }

    private fun generatePage(page: Int, limit: Int): List<VideoItem> {
        return List(limit) { index ->
            val template = mockVideos[(page * limit + index) % mockVideos.size]
            val suffix = "p${page}_$index"
            template.copy(
                id = "${template.id}_$suffix",
                title = "${template.title} · $suffix",
                likeCount = template.likeCount + page * 97 + index,
                favoriteCount = template.favoriteCount + page * 33 + index,
                commentCount = template.commentCount + page * 11 + index
            )
        }
    }

    private fun baseVideo(
        id: String,
        playUrl: String,
        title: String,
        author: String
    ) = VideoItem(
        id = id,
        videoUrl = playUrl,
        title = title,
        authorName = author,
        likeCount = 500,
        commentCount = 48,
        favoriteCount = 120,
        shareCount = 90,
        orientation = VideoOrientation.LANDSCAPE
    )
}


