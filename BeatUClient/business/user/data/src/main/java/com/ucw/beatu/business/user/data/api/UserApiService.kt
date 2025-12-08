package com.ucw.beatu.business.user.data.api

import com.ucw.beatu.business.user.data.api.dto.UserDto
import com.ucw.beatu.shared.common.api.ApiResponse
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

/**
 * 用户API服务接口
 * 连接后端服务的用户相关API端点定义
 */
interface UserApiService {

    /**
     * 获取用户信息
     * GET /api/users/{id}
     */
    @GET("api/users/{id}")
    suspend fun getUserById(@Path("id") userId: String): ApiResponse<UserDto>

    /**
     * 根据用户名获取用户信息
     * GET /api/users/name/{name}
     */
    @GET("api/users/name/{name}")
    suspend fun getUserByName(@Path("name") userName: String): ApiResponse<UserDto>

    /**
     * 关注用户
     * POST /api/users/{id}/follow
     */
    @POST("api/users/{id}/follow")
    suspend fun followUser(@Path("id") userId: String): ApiResponse<Any?>

    /**
     * 取消关注用户
     * POST /api/users/{id}/unfollow
     */
    @POST("api/users/{id}/unfollow")
    suspend fun unfollowUser(@Path("id") userId: String): ApiResponse<Any?>

    /**
     * 获取所有用户信息（首次启动时全量加载）
     * GET /api/users
     */
    @GET("api/users")
    suspend fun getAllUsers(): ApiResponse<List<UserDto>>

    /**
     * 获取指定用户的所有关注关系（首次启动时全量加载）
     * GET /api/users/{id}/follows
     */
    @GET("api/users/{id}/follows")
    suspend fun getUserFollows(@Path("id") userId: String): ApiResponse<List<Map<String, Any>>>
}

