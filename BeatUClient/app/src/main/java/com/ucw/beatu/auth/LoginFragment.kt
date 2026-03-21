package com.ucw.beatu.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.ucw.beatu.R
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class LoginFragment : Fragment() {

    private val viewModel: LoginViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_login, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val etUser = view.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.et_user_name)
        val etPass = view.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.et_password)
        val btn = view.findViewById<com.google.android.material.button.MaterialButton>(R.id.btn_login)
        val tvError = view.findViewById<android.widget.TextView>(R.id.tv_error)
        val progress = view.findViewById<android.widget.ProgressBar>(R.id.progress)

        btn.setOnClickListener {
            tvError.visibility = View.GONE
            viewModel.submit(
                userName = etUser.text?.toString().orEmpty(),
                password = etPass.text?.toString().orEmpty()
            )
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    progress.visibility = if (state.loading) View.VISIBLE else View.GONE
                    btn.isEnabled = !state.loading
                    if (state.error != null) {
                        tvError.text = state.error
                        tvError.visibility = View.VISIBLE
                    }
                    if (state.success) {
                        navigateToFeed()
                    }
                }
            }
        }
    }

    private fun navigateToFeed() {
        findNavController().navigate(R.id.action_login_to_feed)
    }
}
