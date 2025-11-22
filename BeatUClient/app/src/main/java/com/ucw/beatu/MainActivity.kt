package com.ucw.beatu

import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.commit
import com.ucw.beatu.business.videofeed.presentation.ui.FeedFragment
import dagger.hilt.android.AndroidEntryPoint

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
            
            // 显示 FeedFragment
            if (savedInstanceState == null) {
                Log.d(TAG, "onCreate: Adding FeedFragment")
                supportFragmentManager.commit {
                    replace(R.id.fragment_container, FeedFragment())
                }
                Log.d(TAG, "onCreate: FeedFragment added")
            } else {
                Log.d(TAG, "onCreate: Restoring from saved state")
            }
            
            Log.d(TAG, "onCreate: Completed successfully")
        } catch (e: Exception) {
            Log.e(TAG, "onCreate: Error occurred", e)
            throw e
        }
    }
}