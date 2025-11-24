package com.ucw.beatu.business.landscape.presentation.ui

import android.content.pm.ActivityInfo
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.viewpager2.widget.ViewPager2
import com.ucw.beatu.business.landscape.presentation.R
import com.ucw.beatu.business.landscape.presentation.model.VideoItem
import com.ucw.beatu.business.landscape.presentation.ui.adapter.LandscapeVideoAdapter
import com.ucw.beatu.business.landscape.presentation.viewmodel.LandscapeViewModel
import com.ucw.beatu.shared.common.navigation.LandscapeLaunchContract
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import kotlin.collections.isNotEmpty


/**
 * 横屏播放页面
 * - 全屏、黑色背景
 * - 支持上下滑动切换横屏视频
 * - 左上角退出按钮
 * - 集成 ExoPlayer、手势控制、亮度/音量调节
 */
@AndroidEntryPoint
class LandscapeActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "LandscapeActivity"
    }

    private lateinit var viewPager: ViewPager2
    private lateinit var adapter: LandscapeVideoAdapter
    private lateinit var viewModel: LandscapeViewModel
    private var exitButton: ImageButton? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 强制横屏
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        
        setContentView(R.layout.activity_landscape)
        configureImmersiveMode()
        
        // 初始化 ViewModel
        viewModel = ViewModelProvider(this)[LandscapeViewModel::class.java]
        val entryVideo = extractEntryVideo()
        entryVideo?.let { viewModel.showExternalVideo(it) }
        viewModel.loadVideoList()
        
        // 初始化退出按钮
        exitButton = findViewById(R.id.btn_exit_landscape)
        exitButton?.setOnClickListener {
            finish()
        }
        
        // 初始化 ViewPager2
        viewPager = findViewById(R.id.viewpager_landscape)
        viewPager.orientation = ViewPager2.ORIENTATION_VERTICAL
        // 预加载相邻视频（前后各1页）
        viewPager.offscreenPageLimit = 1
        
        // 创建 Adapter
        adapter = LandscapeVideoAdapter(this)
        viewPager.adapter = adapter
        
        // 设置页面切换监听
        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                Log.d(TAG, "Page selected: $position")
                // 如果滑动到最后一个视频，加载更多
                if (position >= adapter.itemCount - 2) {
                    viewModel.loadMoreVideos()
                }
            }
        })
        
        // 观察 ViewModel 状态
        observeViewModel()
    }

    private fun configureImmersiveMode() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.hide(WindowInsetsCompat.Type.systemBars())
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }
    
    private fun observeViewModel() {
        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state: com.ucw.beatu.business.landscape.presentation.viewmodel.LandscapeUiState ->
                    if (state.videoList.isNotEmpty()) {
                        adapter.updateVideoList(state.videoList)
                    }
                }
            }
        }
    }
    
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        super.onBackPressed()
        // 退出横屏模式
        finish()
    }

    private fun extractEntryVideo(): VideoItem? {
        val videoUrl = intent.getStringExtra(LandscapeLaunchContract.EXTRA_VIDEO_URL) ?: return null
        val id = intent.getStringExtra(LandscapeLaunchContract.EXTRA_VIDEO_ID) ?: videoUrl
        val title = intent.getStringExtra(LandscapeLaunchContract.EXTRA_VIDEO_TITLE) ?: ""
        val author = intent.getStringExtra(LandscapeLaunchContract.EXTRA_VIDEO_AUTHOR) ?: ""
        val like = intent.getIntExtra(LandscapeLaunchContract.EXTRA_VIDEO_LIKE, 0)
        val comment = intent.getIntExtra(LandscapeLaunchContract.EXTRA_VIDEO_COMMENT, 0)
        val favorite = intent.getIntExtra(LandscapeLaunchContract.EXTRA_VIDEO_FAVORITE, 0)
        val share = intent.getIntExtra(LandscapeLaunchContract.EXTRA_VIDEO_SHARE, 0)
        return VideoItem(
            id = id,
            videoUrl = videoUrl,
            title = title,
            authorName = author,
            likeCount = like,
            commentCount = comment,
            favoriteCount = favorite,
            shareCount = share
        )
    }
}