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
import com.example.firstproject.databinding.FragmentRegisterBinding
import com.example.firstproject.ui.common.showAeroSnackbar

class RegisterFragment : Fragment() {
    private var _binding: FragmentRegisterBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AuthViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRegisterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.btnRegister.setOnClickListener { submit() }
        binding.etPassword.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                submit()
                true
            } else false
        }
        binding.tvGoToLogin.setOnClickListener {
            findNavController().navigate(R.id.action_registerFragment_to_loginFragment)
        }
        // Clear an inline error as soon as the user edits that field
        binding.etUsername.doAfterTextChanged { binding.tilUsername.error = null }
        binding.etEmail.doAfterTextChanged { binding.tilEmail.error = null }
        binding.etPassword.doAfterTextChanged { binding.tilPassword.error = null }

        viewModel.uiState.observe(viewLifecycleOwner, ::render)
    }

    private fun submit() {
        val username = binding.etUsername.text?.toString()?.trim().orEmpty()
        val email = binding.etEmail.text?.toString()?.trim().orEmpty()
        val password = binding.etPassword.text?.toString().orEmpty()

        val usernameError = AuthInputValidator.validateUsername(username)
        val emailError = AuthInputValidator.validateEmail(email)
        val passwordError = AuthInputValidator.validatePassword(password)
        binding.tilUsername.error = usernameError?.let {
            getString(it, AuthInputValidator.USERNAME_MAX_LENGTH)
        }
        binding.tilEmail.error = emailError?.let(::getString)
        binding.tilPassword.error = passwordError?.let(::getString)
        if (usernameError != null || emailError != null || passwordError != null) return

        hideKeyboard()
        viewModel.signup(username, email, password)
    }

    private fun render(state: AuthUiState) {
        setLoading(state == AuthUiState.Loading)
        when (state) {
            is AuthUiState.Success -> {
                viewModel.onStateHandled()
                if (findNavController().currentDestination?.id == R.id.registerFragment) {
                    findNavController().navigate(R.id.action_registerFragment_to_homeFragment)
                }
                showAeroSnackbar(
                    state.username?.let { getString(R.string.welcome_new_user, it) }
                        ?: getString(R.string.welcome_new)
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
        binding.progressRegister.isVisible = loading
        binding.btnRegister.isEnabled = !loading
        binding.btnRegister.text = if (loading) null else getString(R.string.action_register)
        binding.tilUsername.isEnabled = !loading
        binding.tilEmail.isEnabled = !loading
        binding.tilPassword.isEnabled = !loading
        binding.tvGoToLogin.isEnabled = !loading
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
