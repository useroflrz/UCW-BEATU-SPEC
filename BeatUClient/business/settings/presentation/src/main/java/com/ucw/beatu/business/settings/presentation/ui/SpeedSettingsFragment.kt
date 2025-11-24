package com.ucw.beatu.business.settings.presentation.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.ucw.beatu.business.settings.presentation.R
import com.ucw.beatu.business.settings.presentation.databinding.FragmentSpeedSettingsBinding
import com.ucw.beatu.business.settings.presentation.databinding.ItemSettingsArrowBinding

/**
 * 倍速设置 Fragment
 * 纯表现层，使用 Mock 数据展示
 */
class SpeedSettingsFragment : Fragment() {
    
    private var _binding: FragmentSpeedSettingsBinding? = null
    private val binding get() = _binding!!
    
    // Mock 当前选中的倍速
    private var selectedSpeed = 1.0f
    
    private val speedOptions = listOf(
        3.0f to R.string.speed_3x,
        2.0f to R.string.speed_2x,
        1.5f to R.string.speed_1_5x,
        1.25f to R.string.speed_1_25x,
        1.0f to R.string.speed_1x,
        0.75f to R.string.speed_0_75x
    )
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSpeedSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViews()
        updateUI()
    }
    
    private fun setupViews() {
        // 绑定倍速选项
        val bindings = listOf(
            binding.itemSpeed3x,
            binding.itemSpeed2x,
            binding.itemSpeed15x,
            binding.itemSpeed125x,
            binding.itemSpeed1x,
            binding.itemSpeed075x
        )
        
        speedOptions.forEachIndexed { index, (speed, stringRes) ->
            val itemBinding = ItemSettingsArrowBinding.bind(bindings[index].root)
            itemBinding.title.setText(stringRes)
            itemBinding.icon.visibility = View.GONE
            
            itemBinding.root.setOnClickListener {
                selectedSpeed = speed
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
            binding.itemSpeed3x,
            binding.itemSpeed2x,
            binding.itemSpeed15x,
            binding.itemSpeed125x,
            binding.itemSpeed1x,
            binding.itemSpeed075x
        )
        
        speedOptions.forEachIndexed { index, (speed, _) ->
            val itemBinding = ItemSettingsArrowBinding.bind(bindings[index].root)
            if (selectedSpeed == speed) {
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
