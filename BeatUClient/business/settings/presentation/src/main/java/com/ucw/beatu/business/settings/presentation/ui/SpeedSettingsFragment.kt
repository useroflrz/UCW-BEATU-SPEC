package com.ucw.beatu.business.settings.presentation.ui

import android.os.Bundle
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.ucw.beatu.business.settings.presentation.R
import com.ucw.beatu.business.settings.presentation.databinding.FragmentSpeedSettingsBinding
import com.ucw.beatu.business.settings.presentation.databinding.ItemSettingsArrowBinding
import com.ucw.beatu.business.settings.presentation.viewmodel.SettingsViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/**
 * 倍速设置 Fragment（与 SettingsViewModel 共享状态）
 */
@AndroidEntryPoint
class SpeedSettingsFragment : Fragment() {

    private val viewModel: SettingsViewModel by activityViewModels()

    private var _binding: FragmentSpeedSettingsBinding? = null
    private val binding get() = _binding!!

    private lateinit var optionBindings: List<ItemSettingsArrowBinding>

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
        bindOptions()
        observeState()
    }

    private fun bindOptions() {
        optionBindings = listOf(
            ItemSettingsArrowBinding.bind(binding.itemSpeed3x.root).apply {
                title.setText(R.string.speed_3x)
                icon.visibility = View.GONE
            },
            ItemSettingsArrowBinding.bind(binding.itemSpeed2x.root).apply {
                title.setText(R.string.speed_2x)
                icon.visibility = View.GONE
            },
            ItemSettingsArrowBinding.bind(binding.itemSpeed15x.root).apply {
                title.setText(R.string.speed_1_5x)
                icon.visibility = View.GONE
            },
            ItemSettingsArrowBinding.bind(binding.itemSpeed125x.root).apply {
                title.setText(R.string.speed_1_25x)
                icon.visibility = View.GONE
            },
            ItemSettingsArrowBinding.bind(binding.itemSpeed1x.root).apply {
                title.setText(R.string.speed_1x)
                icon.visibility = View.GONE
            },
            ItemSettingsArrowBinding.bind(binding.itemSpeed075x.root).apply {
                title.setText(R.string.speed_0_75x)
                icon.visibility = View.GONE
            }
        )

        optionBindings.forEachIndexed { index, option ->
            option.root.setOnClickListener {
                it.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                val targetSpeed = SettingsViewModel.SPEED_OPTIONS[index]
                viewModel.updateDefaultSpeed(targetSpeed)
                navigateBack()
            }
        }
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    optionBindings.forEachIndexed { index, option ->
                        val speed = SettingsViewModel.SPEED_OPTIONS[index]
                        if (state.defaultSpeed == speed) {
                            option.arrow.setImageResource(R.drawable.ic_check)
                            option.arrow.visibility = View.VISIBLE
                        } else {
                            option.arrow.visibility = View.GONE
                        }
                    }
                }
            }
        }
    }

    private fun navigateBack() {
        (activity as? SettingsTestActivity)?.showSettingsFragment()
            ?: runCatching { findNavController().popBackStack() }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

