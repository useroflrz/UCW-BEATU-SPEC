package com.ucw.beatu

import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import dagger.hilt.android.AndroidEntryPoint

/**
 * MainActivity 作为应用的入口 Activity
 * 使用 NavHostFragment 作为导航容器，由 Navigation Graph 自动管理 Fragment 栈
 */
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    
    companion object {
        private const val TAG = "MainActivity"
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate: Starting MainActivity")
        
        try {
            enableEdgeToEdge()
            setContentView(R.layout.activity_main)
            Log.d(TAG, "onCreate: Content view set")
            
            ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
                val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
                insets
            }
            
            // NavHostFragment 会自动根据 main_nav_graph 的 startDestination 加载 FeedFragment
            // 不需要手动 replace Fragment，让 Navigation 系统自动管理栈
            Log.d(TAG, "onCreate: NavHostFragment will handle Fragment management")
            
            Log.d(TAG, "onCreate: Completed successfully")
        } catch (e: Exception) {
            Log.e(TAG, "onCreate: Error occurred", e)
            throw e
        }
    }
}