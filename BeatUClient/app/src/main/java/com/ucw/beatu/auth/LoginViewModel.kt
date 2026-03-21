package com.ucw.beatu.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ucw.beatu.business.user.data.api.UserApiService
import com.ucw.beatu.business.user.data.api.dto.LoginRequestDto
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    retrofit: Retrofit,
    private val authSessionStore: AuthSessionStore
) : ViewModel() {

    private val api = retrofit.create(UserApiService::class.java)

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun submit(userName: String, password: String) {
        val name = userName.trim()
        if (name.isBlank() || password.length < 6) {
            _uiState.value = LoginUiState(error = "请输入用户名和至少 6 位密码")
            return
        }
        viewModelScope.launch {
            _uiState.value = LoginUiState(loading = true)
            runCatching {
                val resp = api.loginOrRegister(LoginRequestDto(userName = name, password = password))
                if (!resp.isSuccess || resp.data == null) {
                    throw IllegalStateException(resp.message.ifBlank { "请求失败" })
                }
                val body = resp.data!!
                authSessionStore.save(
                    userId = body.userId,
                    userName = body.userName,
                    accessToken = body.accessToken.ifBlank { "" }
                )
                _uiState.value = LoginUiState(loading = false, success = true)
            }.onFailure { e ->
                _uiState.value = LoginUiState(
                    loading = false,
                    error = e.message ?: "登录失败"
                )
            }
        }
    }
}

data class LoginUiState(
    val loading: Boolean = false,
    val success: Boolean = false,
    val error: String? = null
)
