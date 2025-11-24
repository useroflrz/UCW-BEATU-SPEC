package com.ucw.beatu.business.settings.presentation.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.ucw.beatu.business.settings.presentation.R
import com.ucw.beatu.business.settings.presentation.databinding.FragmentSettingsBinding
import com.ucw.beatu.business.settings.presentation.databinding.ItemSettingsArrowBinding
import com.ucw.beatu.business.settings.presentation.databinding.ItemSettingsSwitchBinding

/**
 * 设置页面 Fragment
 * 纯表现层，使用 Mock 数据展示
 */
class SettingsFragment : Fragment() {
    
    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!
    
    // Mock 数据
    private var aiSearchEnabled = false
    private var aiCommentEnabled = false
    private var autoPlayEnabled = true
    private var currentSpeed = 1.0f
    private var currentQuality = "自动"
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViews()
        updateUI()
    }
    
    private fun setupViews() {
        // 设置返回按钮点击事件
        binding.btnBack.setOnClickListener {
            // 如果是在 SettingsTestActivity 中，关闭 Activity
            if (activity is SettingsTestActivity) {
                activity?.finish()
            } else {
                // 如果不在测试 Activity 中，尝试使用 Navigation 返回
                try {
                    if (!findNavController().popBackStack()) {
                        // 如果无法返回，关闭 Activity
                        activity?.finish()
                    }
                } catch (e: Exception) {
                    // Navigation 未配置，关闭 Activity
                    activity?.finish()
                }
            }
        }
        
        // 绑定 AI 搜索开关
        val aiSearchBinding = ItemSettingsSwitchBinding.bind(binding.itemAiSearch.root)
        aiSearchBinding.icon.setImageResource(R.drawable.ic_settings_search)
        aiSearchBinding.title.setText(R.string.settings_ai_search)
        
        // 绑定 AI 评论开关
        val aiCommentBinding = ItemSettingsSwitchBinding.bind(binding.itemAiComment.root)
        aiCommentBinding.icon.setImageResource(R.drawable.ic_settings_comment)
        aiCommentBinding.title.setText(R.string.settings_ai_comment)
        
        // 绑定自动播放开关
        val autoPlayBinding = ItemSettingsSwitchBinding.bind(binding.itemAutoPlay.root)
        autoPlayBinding.icon.setImageResource(R.drawable.ic_settings_play)
        autoPlayBinding.title.setText(R.string.settings_auto_play)
        
        // 绑定倍速设置
        val speedBinding = ItemSettingsArrowBinding.bind(binding.itemSpeed.root)
        speedBinding.icon.setImageResource(R.drawable.ic_speed)
        speedBinding.title.setText(R.string.settings_speed)
        
        // 绑定清晰度设置
        val qualityBinding = ItemSettingsArrowBinding.bind(binding.itemQuality.root)
        qualityBinding.icon.setImageResource(R.drawable.ic_settings_quality)
        qualityBinding.title.setText(R.string.settings_quality)
        
        // 绑定关于
        val aboutBinding = ItemSettingsArrowBinding.bind(binding.itemAbout.root)
        aboutBinding.icon.setImageResource(R.drawable.ic_settings_about)
        aboutBinding.title.setText(R.string.settings_about)
        
        // 设置点击事件
        aiSearchBinding.root.setOnClickListener {
            aiSearchEnabled = !aiSearchEnabled
            aiSearchBinding.switchToggle.isChecked = aiSearchEnabled
        }
        
        aiCommentBinding.root.setOnClickListener {
            aiCommentEnabled = !aiCommentEnabled
            aiCommentBinding.switchToggle.isChecked = aiCommentEnabled
        }
        
        autoPlayBinding.root.setOnClickListener {
            autoPlayEnabled = !autoPlayEnabled
            autoPlayBinding.switchToggle.isChecked = autoPlayEnabled
        }
        
        speedBinding.root.setOnClickListener {
            // 通过 Activity 跳转到倍速设置页面
            if (activity is SettingsTestActivity) {
                (activity as SettingsTestActivity).showSpeedSettingsFragment()
            }
        }
        
        qualityBinding.root.setOnClickListener {
            // 通过 Activity 跳转到清晰度设置页面
            if (activity is SettingsTestActivity) {
                (activity as SettingsTestActivity).showQualitySettingsFragment()
            }
        }
        
        aboutBinding.root.setOnClickListener {
            // TODO: 导航到关于页面
        }
    }
    
    private fun updateUI() {
        // 更新 AI 搜索开关
        val aiSearchBinding = ItemSettingsSwitchBinding.bind(binding.itemAiSearch.root)
        aiSearchBinding.switchToggle.isChecked = aiSearchEnabled
        
        // 更新 AI 评论开关
        val aiCommentBinding = ItemSettingsSwitchBinding.bind(binding.itemAiComment.root)
        aiCommentBinding.switchToggle.isChecked = aiCommentEnabled
        
        // 更新自动播放开关
        val autoPlayBinding = ItemSettingsSwitchBinding.bind(binding.itemAutoPlay.root)
        autoPlayBinding.switchToggle.isChecked = autoPlayEnabled
        
        // 更新倍速显示
        val speedBinding = ItemSettingsArrowBinding.bind(binding.itemSpeed.root)
        val speedText = when (currentSpeed) {
            3.0f -> getString(R.string.speed_3x)
            2.0f -> getString(R.string.speed_2x)
            1.5f -> getString(R.string.speed_1_5x)
            1.25f -> getString(R.string.speed_1_25x)
            1.0f -> getString(R.string.speed_1x)
            0.75f -> getString(R.string.speed_0_75x)
            else -> "${currentSpeed}x"
        }
        speedBinding.subtitle.text = speedText
        speedBinding.subtitle.visibility = View.VISIBLE
        
        // 更新清晰度显示
        val qualityBinding = ItemSettingsArrowBinding.bind(binding.itemQuality.root)
        val qualityText = when (currentQuality) {
            "1080P 高清" -> getString(R.string.quality_1080p)
            "720P 准高清" -> getString(R.string.quality_720p)
            "480P 标清" -> getString(R.string.quality_480p)
            "360P 流畅" -> getString(R.string.quality_360p)
            "自动" -> getString(R.string.quality_auto)
            else -> currentQuality
        }
        qualityBinding.subtitle.text = qualityText
        qualityBinding.subtitle.visibility = View.VISIBLE
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
