package com.ucw.beatu.auth

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.ucw.beatu.R
import com.ucw.beatu.business.user.data.api.UserApiService
import com.ucw.beatu.business.user.data.api.dto.LoginRequestDto
import com.ucw.beatu.business.user.data.api.dto.RegisterRequestDto
import com.ucw.beatu.shared.common.threading.SuspendBridge
import com.ucw.beatu.shared.common.threading.TaskNames
import com.ucw.beatu.shared.common.threading.TaskRunner
import dagger.hilt.android.AndroidEntryPoint
import retrofit2.Retrofit
import javax.inject.Inject

@AndroidEntryPoint
class AuthActivity : AppCompatActivity() {

    @Inject
    lateinit var retrofit: Retrofit

    @Inject
    lateinit var authSessionStore: AuthSessionStore

    private lateinit var etUserName: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnLogin: Button
    private lateinit var btnRegister: Button
    private lateinit var tvStatus: TextView

    private var authTask: TaskRunner.Handle? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_auth)
        etUserName = findViewById(R.id.et_user_name)
        etPassword = findViewById(R.id.et_password)
        btnLogin = findViewById(R.id.btn_login)
        btnRegister = findViewById(R.id.btn_register)
        tvStatus = findViewById(R.id.tv_auth_status)

        btnLogin.setOnClickListener { login() }
        btnRegister.setOnClickListener { register() }
        tvStatus.text = "当前用户：${authSessionStore.userNameOrDefault()}"
    }

    private fun login() {
        val userName = etUserName.text.toString().trim()
        val password = etPassword.text.toString()
        if (userName.isBlank() || password.length < 6) {
            Toast.makeText(this, "请输入用户名和至少6位密码", Toast.LENGTH_SHORT).show()
            return
        }
        authTask?.cancel()
        authTask = TaskRunner.run(
            name = TaskNames.of("ui_auth", "login"),
            timeoutMs = 10_000L
        ) {
            runCatching {
                val api = retrofit.create(UserApiService::class.java)
                val response = SuspendBridge.runBlocking {
                    api.login(LoginRequestDto(userName = userName, password = password))
                }
                val body = response.data ?: throw IllegalStateException("登录失败")
                authSessionStore.save(body.userId, body.userName, body.accessToken)
                runOnUiThread {
                    Toast.makeText(this, "登录成功：${body.userName}", Toast.LENGTH_SHORT).show()
                    tvStatus.text = "当前用户：${body.userName}"
                    finish()
                }
            }.onFailure { e ->
                runOnUiThread {
                    Toast.makeText(this, "登录失败：${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun register() {
        val userName = etUserName.text.toString().trim()
        val password = etPassword.text.toString()
        if (userName.isBlank() || password.length < 6) {
            Toast.makeText(this, "请输入用户名和至少6位密码", Toast.LENGTH_SHORT).show()
            return
        }
        authTask?.cancel()
        authTask = TaskRunner.run(
            name = TaskNames.of("ui_auth", "register"),
            timeoutMs = 10_000L
        ) {
            runCatching {
                val api = retrofit.create(UserApiService::class.java)
                val response = SuspendBridge.runBlocking {
                    api.register(RegisterRequestDto(userName = userName, password = password))
                }
                val body = response.data ?: throw IllegalStateException("注册失败")
                authSessionStore.save(body.userId, body.userName, body.accessToken)
                runOnUiThread {
                    Toast.makeText(this, "注册成功：${body.userName}", Toast.LENGTH_SHORT).show()
                    tvStatus.text = "当前用户：${body.userName}"
                    finish()
                }
            }.onFailure { e ->
                runOnUiThread {
                    Toast.makeText(this, "注册失败：${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onDestroy() {
        authTask?.cancel()
        super.onDestroy()
    }
}

