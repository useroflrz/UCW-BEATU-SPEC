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
import com.ucw.beatu.business.settings.domain.model.PlaybackQualityPreference
import com.ucw.beatu.business.settings.presentation.R
import com.ucw.beatu.business.settings.presentation.databinding.FragmentQualitySettingsBinding
import com.ucw.beatu.business.settings.presentation.databinding.ItemSettingsArrowBinding
import com.ucw.beatu.business.settings.presentation.viewmodel.SettingsViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/**
 * 清晰度设置 Fragment
 */
@AndroidEntryPoint
class QualitySettingsFragment : Fragment() {

    private val viewModel: SettingsViewModel by activityViewModels()

    private var _binding: FragmentQualitySettingsBinding? = null
    private val binding get() = _binding!!

    private lateinit var optionBindings: Map<PlaybackQualityPreference, ItemSettingsArrowBinding>

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
        bindOptions()
        observeState()
    }

    private fun bindOptions() {
        optionBindings = mapOf(
            PlaybackQualityPreference.HD_1080 to ItemSettingsArrowBinding.bind(binding.itemQuality1080p.root),
            PlaybackQualityPreference.HD_720 to ItemSettingsArrowBinding.bind(binding.itemQuality720p.root),
            PlaybackQualityPreference.SD_480 to ItemSettingsArrowBinding.bind(binding.itemQuality480p.root),
            PlaybackQualityPreference.SD_360 to ItemSettingsArrowBinding.bind(binding.itemQuality360p.root),
            PlaybackQualityPreference.AUTO to ItemSettingsArrowBinding.bind(binding.itemQualityAuto.root)
        )

        optionBindings.forEach { (quality, binding) ->
            binding.title.text = quality.label
            binding.icon.visibility = View.GONE
            binding.root.setOnClickListener {
                it.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                viewModel.updateDefaultQuality(quality)
                navigateBack()
            }
        }
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    optionBindings.forEach { (quality, binding) ->
                        if (state.defaultQuality == quality) {
                            binding.arrow.setImageResource(R.drawable.ic_check)
                            binding.arrow.visibility = View.VISIBLE
                        } else {
                            binding.arrow.visibility = View.GONE
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

