package com.ucw.beatu.business.settings.presentation.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.ucw.beatu.business.settings.presentation.R
import com.ucw.beatu.business.settings.presentation.databinding.FragmentQualitySettingsBinding
import com.ucw.beatu.business.settings.presentation.databinding.ItemSettingsArrowBinding

/**
 * 清晰度设置 Fragment
 * 纯表现层，使用 Mock 数据展示
 */
class QualitySettingsFragment : Fragment() {
    
    private var _binding: FragmentQualitySettingsBinding? = null
    private val binding get() = _binding!!
    
    // Mock 当前选中的清晰度
    private var selectedQuality = "自动"
    
    private val qualityOptions = listOf(
        "1080P 高清" to R.string.quality_1080p,
        "720P 准高清" to R.string.quality_720p,
        "480P 标清" to R.string.quality_480p,
        "360P 流畅" to R.string.quality_360p,
        "自动" to R.string.quality_auto
    )
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentQualitySettingsBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViews()
        updateUI()
    }
    
    private fun setupViews() {
        // 绑定清晰度选项
        val bindings = listOf(
            binding.itemQuality1080p,
            binding.itemQuality720p,
            binding.itemQuality480p,
            binding.itemQuality360p,
            binding.itemQualityAuto
        )
        
        qualityOptions.forEachIndexed { index, (quality, stringRes) ->
            val itemBinding = ItemSettingsArrowBinding.bind(bindings[index].root)
            itemBinding.title.setText(stringRes)
            itemBinding.icon.visibility = View.GONE
            
            itemBinding.root.setOnClickListener {
                selectedQuality = quality
                updateUI()
                // 返回上一页
                if (activity is SettingsTestActivity) {
                    (activity as SettingsTestActivity).showSettingsFragment()
                } else {
                    // 如果不在 SettingsTestActivity 中，尝试使用 Navigation
                    try {
                        findNavController().popBackStack()
                    } catch (e: Exception) {
                        // Navigation 未配置，忽略
                    }
                }
            }
        }
    }
    
    private fun updateUI() {
        // 更新选中状态
        val bindings = listOf(
            binding.itemQuality1080p,
            binding.itemQuality720p,
            binding.itemQuality480p,
            binding.itemQuality360p,
            binding.itemQualityAuto
        )
        
        qualityOptions.forEachIndexed { index, (quality, _) ->
            val itemBinding = ItemSettingsArrowBinding.bind(bindings[index].root)
            if (selectedQuality == quality) {
                // 显示选中指示器（对勾）
                itemBinding.arrow.setImageResource(R.drawable.ic_check)
                itemBinding.arrow.visibility = View.VISIBLE
                itemBinding.arrow.contentDescription = "已选中"
            } else {
                itemBinding.arrow.visibility = View.GONE
            }
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
