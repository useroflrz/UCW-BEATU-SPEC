package com.ucw.beatu.business.settings.presentation.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.commit
import com.ucw.beatu.business.settings.presentation.R
import dagger.hilt.android.AndroidEntryPoint

/**
 * 设置页面测试 Activity
 * 用于测试所有设置相关的 Fragment 页面
 * 
 * 功能：
 * 1. 默认显示主设置页面
 * 2. 支持从主设置页面内部跳转到子设置页面
 */
@AndroidEntryPoint
class SettingsTestActivity : AppCompatActivity() {
    
    // 当前显示的 Fragment 类型
    private var currentFragmentType: FragmentType = FragmentType.SETTINGS
    
    private enum class FragmentType {
        SETTINGS,
        SPEED,
        QUALITY
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings_test)
        
        // 默认显示主设置页面
        if (savedInstanceState == null) {
            showSettingsFragment()
        }
    }
    
    /**
     * 显示主设置页面
     */
    fun showSettingsFragment() {
        currentFragmentType = FragmentType.SETTINGS
        
        supportFragmentManager.commit {
            replace(R.id.fragment_container, SettingsFragment())
        }
    }
    
    /**
     * 显示倍速设置页面
     */
    fun showSpeedSettingsFragment() {
        currentFragmentType = FragmentType.SPEED
        
        supportFragmentManager.commit {
            replace(R.id.fragment_container, SpeedSettingsFragment())
        }
    }
    
    /**
     * 显示清晰度设置页面
     */
    fun showQualitySettingsFragment() {
        currentFragmentType = FragmentType.QUALITY
        
        supportFragmentManager.commit {
            replace(R.id.fragment_container, QualitySettingsFragment())
        }
    }
}
