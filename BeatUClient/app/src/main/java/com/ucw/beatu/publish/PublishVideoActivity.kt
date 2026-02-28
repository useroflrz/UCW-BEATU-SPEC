package com.ucw.beatu.publish

import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.ucw.beatu.R
import com.ucw.beatu.business.videofeed.data.api.VideoFeedApiService
import com.ucw.beatu.shared.common.threading.SuspendBridge
import com.ucw.beatu.shared.common.threading.TaskNames
import com.ucw.beatu.shared.common.threading.TaskRunner
import dagger.hilt.android.AndroidEntryPoint
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Retrofit
import javax.inject.Inject

@AndroidEntryPoint
class PublishVideoActivity : AppCompatActivity() {

    @Inject
    lateinit var retrofit: Retrofit

    private lateinit var etTitle: EditText
    private lateinit var tvPicked: TextView
    private lateinit var btnPick: Button
    private lateinit var btnPublish: Button

    private var pickedUri: Uri? = null
    private var publishTask: TaskRunner.Handle? = null

    private val pickVideoLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        pickedUri = uri
        tvPicked.text = uri?.toString() ?: "未选择视频"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_publish_video)
        etTitle = findViewById(R.id.et_publish_title)
        tvPicked = findViewById(R.id.tv_publish_picked)
        btnPick = findViewById(R.id.btn_pick_video)
        btnPublish = findViewById(R.id.btn_publish_video)

        btnPick.setOnClickListener {
            pickVideoLauncher.launch("video/*")
        }
        btnPublish.setOnClickListener {
            publish()
        }
    }

    private fun publish() {
        val title = etTitle.text.toString().trim()
        val uri = pickedUri
        if (title.isBlank() || uri == null) {
            Toast.makeText(this, "请填写标题并选择视频", Toast.LENGTH_SHORT).show()
            return
        }
        val bytes = contentResolver.openInputStream(uri)?.use { it.readBytes() }
        if (bytes == null || bytes.isEmpty()) {
            Toast.makeText(this, "读取视频失败", Toast.LENGTH_SHORT).show()
            return
        }
        publishTask?.cancel()
        publishTask = TaskRunner.run(
            name = TaskNames.of("ui_publish", "publish_video"),
            timeoutMs = 60_000L
        ) {
            runCatching {
                val api = retrofit.create(VideoFeedApiService::class.java)
                val mime = contentResolver.getType(uri) ?: "video/mp4"
                val filePart = MultipartBody.Part.createFormData(
                    "file",
                    "upload.mp4",
                    bytes.toRequestBody(mime.toMediaTypeOrNull())
                )
                val titleBody = title.toRequestBody("text/plain".toMediaTypeOrNull())
                val orientationBody = "portrait".toRequestBody("text/plain".toMediaTypeOrNull())
                val response = SuspendBridge.runBlocking {
                    api.publishVideo(filePart, titleBody, orientationBody)
                }
                val data = response.data ?: throw IllegalStateException("发布失败")
                runOnUiThread {
                    Toast.makeText(this, "发布成功，videoId=${data.videoId}", Toast.LENGTH_LONG).show()
                    finish()
                }
            }.onFailure { e ->
                runOnUiThread {
                    Toast.makeText(this, "发布失败：${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onDestroy() {
        publishTask?.cancel()
        super.onDestroy()
    }
}

