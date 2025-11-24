package com.ucw.beatu.business.settings.presentation.ui

import android.os.Bundle
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import com.ucw.beatu.business.settings.presentation.R
import com.ucw.beatu.business.settings.presentation.databinding.FragmentSettingsBinding
import com.ucw.beatu.business.settings.presentation.databinding.ItemSettingsArrowBinding
import com.ucw.beatu.business.settings.presentation.databinding.ItemSettingsSwitchBinding
import com.ucw.beatu.business.settings.presentation.viewmodel.SettingField
import com.ucw.beatu.business.settings.presentation.viewmodel.SettingsEvent
import com.ucw.beatu.business.settings.presentation.viewmodel.SettingsViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/**
 * 设置页面 Fragment（iOS 风格卡片 + DataStore 持久化）
 */
@AndroidEntryPoint
class SettingsFragment : Fragment() {

    private val viewModel: SettingsViewModel by activityViewModels()

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private lateinit var aiSearchBinding: ItemSettingsSwitchBinding
    private lateinit var aiCommentBinding: ItemSettingsSwitchBinding
    private lateinit var autoPlayBinding: ItemSettingsSwitchBinding
    private lateinit var speedBinding: ItemSettingsArrowBinding
    private lateinit var qualityBinding: ItemSettingsArrowBinding
    private lateinit var aboutBinding: ItemSettingsArrowBinding

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
        bindCards()
        setupClickListeners()
        observeUi()
        observeEvents()
    }

    private fun bindCards() {
        aiSearchBinding = ItemSettingsSwitchBinding.bind(binding.itemAiSearch.root).apply {
            icon.setImageResource(R.drawable.ic_settings_search)
            title.setText(R.string.settings_ai_search)
            switchToggle.isClickable = false
            switchToggle.isFocusable = false
        }
        aiCommentBinding = ItemSettingsSwitchBinding.bind(binding.itemAiComment.root).apply {
            icon.setImageResource(R.drawable.ic_settings_comment)
            title.setText(R.string.settings_ai_comment)
            switchToggle.isClickable = false
            switchToggle.isFocusable = false
        }
        autoPlayBinding = ItemSettingsSwitchBinding.bind(binding.itemAutoPlay.root).apply {
            icon.setImageResource(R.drawable.ic_settings_play)
            title.setText(R.string.settings_auto_play)
            switchToggle.isClickable = false
            switchToggle.isFocusable = false
        }
        speedBinding = ItemSettingsArrowBinding.bind(binding.itemSpeed.root).apply {
            icon.setImageResource(R.drawable.ic_speed)
            title.setText(R.string.settings_speed)
        }
        qualityBinding = ItemSettingsArrowBinding.bind(binding.itemQuality.root).apply {
            icon.setImageResource(R.drawable.ic_settings_quality)
            title.setText(R.string.settings_quality)
        }
        aboutBinding = ItemSettingsArrowBinding.bind(binding.itemAbout.root).apply {
            icon.setImageResource(R.drawable.ic_settings_about)
            title.setText(R.string.settings_about)
            arrow.visibility = View.GONE
        }

        binding.btnBack.setOnClickListener {
            performBackNavigation()
        }
    }

    private fun setupClickListeners() {
        aiSearchBinding.root.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            viewModel.toggleAiSearch(!aiSearchBinding.switchToggle.isChecked)
        }
        aiCommentBinding.root.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            viewModel.toggleAiComment(!aiCommentBinding.switchToggle.isChecked)
        }
        autoPlayBinding.root.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            viewModel.toggleAutoPlay(!autoPlayBinding.switchToggle.isChecked)
        }
        speedBinding.root.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            openSpeedSettings()
        }
        qualityBinding.root.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            openQualitySettings()
        }
        aboutBinding.root.setOnClickListener {
            Snackbar.make(binding.root, "关于信息即将上线", Snackbar.LENGTH_SHORT).show()
        }
    }

    private fun observeUi() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    aiSearchBinding.switchToggle.isChecked = state.aiSearchEnabled
                    aiCommentBinding.switchToggle.isChecked = state.aiCommentEnabled
                    autoPlayBinding.switchToggle.isChecked = state.autoPlayEnabled
                    speedBinding.subtitle.apply {
                        text = formatSpeed(state.defaultSpeed)
                        visibility = View.VISIBLE
                    }
                    qualityBinding.subtitle.apply {
                        text = state.defaultQuality.label
                        visibility = View.VISIBLE
                    }
                    binding.tvLatency.isVisible = state.lastLatencyMs != null
                    state.lastLatencyMs?.let { latency ->
                        binding.tvLatency.text =
                            getString(R.string.settings_latency_hint, latency)
                    }
                }
            }
        }
    }

    private fun observeEvents() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.events.collect { event ->
                    when (event) {
                        is SettingsEvent.ShowMessage -> Snackbar.make(
                            binding.root,
                            event.message,
                            Snackbar.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }
    }

    private fun openSpeedSettings() {
        if (navigateByActionName("action_settingsFragment_to_speedSettingsFragment")) return
        (activity as? SettingsTestActivity)?.showSpeedSettingsFragment()
    }

    private fun openQualitySettings() {
        if (navigateByActionName("action_settingsFragment_to_qualitySettingsFragment")) return
        (activity as? SettingsTestActivity)?.showQualitySettingsFragment()
    }

    private fun navigateByActionName(actionName: String): Boolean {
        val resId = resources.getIdentifier(
            actionName,
            "id",
            requireContext().packageName
        )
        if (resId == 0) return false
        return runCatching {
            findNavController().navigate(resId)
            true
        }.getOrDefault(false)
    }

    private fun performBackNavigation() {
        if (activity is SettingsTestActivity) {
            activity?.finish()
            return
        }
        runCatching {
            if (!findNavController().popBackStack()) {
                activity?.finish()
            }
        }.onFailure {
            activity?.finish()
        }
    }

    private fun formatSpeed(speed: Float): String = when (speed) {
        3.0f -> getString(R.string.speed_3x)
        2.0f -> getString(R.string.speed_2x)
        1.5f -> getString(R.string.speed_1_5x)
        1.25f -> getString(R.string.speed_1_25x)
        1.0f -> getString(R.string.speed_1x)
        0.75f -> getString(R.string.speed_0_75x)
        else -> "${speed}x"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

