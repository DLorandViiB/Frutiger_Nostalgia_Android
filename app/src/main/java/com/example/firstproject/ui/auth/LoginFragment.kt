package com.example.firstproject.ui.auth

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.firstproject.R
import com.example.firstproject.databinding.FragmentLoginBinding
import com.example.firstproject.ui.common.showAeroSnackbar

class LoginFragment : Fragment() {
    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AuthViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.btnLogin.setOnClickListener { submit() }
        binding.etPassword.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                submit()
                true
            } else false
        }
        binding.tvGoToRegister.setOnClickListener {
            findNavController().navigate(R.id.action_loginFragment_to_registerFragment)
        }
        // Clear an inline error as soon as the user edits that field
        binding.etEmail.doAfterTextChanged { binding.tilEmail.error = null }
        binding.etPassword.doAfterTextChanged { binding.tilPassword.error = null }

        viewModel.uiState.observe(viewLifecycleOwner, ::render)
    }

    private fun submit() {
        val email = binding.etEmail.text?.toString()?.trim().orEmpty()
        val password = binding.etPassword.text?.toString().orEmpty()

        val emailError = AuthInputValidator.validateEmail(email)
        val passwordError = AuthInputValidator.validatePassword(password)
        binding.tilEmail.error = emailError?.let(::getString)
        binding.tilPassword.error = passwordError?.let(::getString)
        if (emailError != null || passwordError != null) return

        hideKeyboard()
        viewModel.login(email, password)
    }

    private fun render(state: AuthUiState) {
        setLoading(state == AuthUiState.Loading)
        when (state) {
            is AuthUiState.Success -> {
                viewModel.onStateHandled()
                if (findNavController().currentDestination?.id == R.id.loginFragment) {
                    findNavController().navigate(R.id.action_loginFragment_to_homeFragment)
                }
                showAeroSnackbar(
                    state.username?.let { getString(R.string.welcome_back_user, it) }
                        ?: getString(R.string.welcome_back)
                )
            }
            is AuthUiState.Error -> {
                viewModel.onStateHandled()
                showAeroSnackbar(state.message, isError = true)
            }
            AuthUiState.Idle, AuthUiState.Loading -> Unit
        }
    }

    private fun setLoading(loading: Boolean) {
        binding.progressLogin.isVisible = loading
        binding.btnLogin.isEnabled = !loading
        binding.btnLogin.text = if (loading) null else getString(R.string.action_login)
        binding.tilEmail.isEnabled = !loading
        binding.tilPassword.isEnabled = !loading
        binding.tvGoToRegister.isEnabled = !loading
    }

    private fun hideKeyboard() {
        WindowCompat.getInsetsController(requireActivity().window, binding.root)
            .hide(WindowInsetsCompat.Type.ime())
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
