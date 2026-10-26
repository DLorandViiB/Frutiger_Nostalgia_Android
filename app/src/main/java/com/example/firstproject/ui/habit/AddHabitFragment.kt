package com.example.firstproject.ui.habit

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.core.os.bundleOf
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.firstproject.R
import com.example.firstproject.databinding.FragmentAddHabitBinding
import com.example.firstproject.ui.common.navigateToLoginAfterSessionExpired
import com.example.firstproject.ui.common.showAeroSnackbar
import com.example.firstproject.ui.schedule.CreateScheduleFragment

class AddHabitFragment : Fragment() {
    private var _binding: FragmentAddHabitBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AddHabitViewModel by viewModels()
    private val categoryAdapter = CategoryTileAdapter { viewModel.selectCategory(it.id) }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddHabitBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.rvCategories.adapter = categoryAdapter

        binding.btnCreateHabit.setOnClickListener { submit() }
        binding.etGoal.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                hideKeyboard()
                true
            } else false
        }
        binding.btnRetryCategories.setOnClickListener { viewModel.loadCategories() }
        binding.tvScheduleExisting.setOnClickListener { openCreateSchedule(habitId = null) }
        // Clear an inline error as soon as the user edits that field
        binding.etName.doAfterTextChanged { binding.tilName.error = null }
        binding.etGoal.doAfterTextChanged { binding.tilGoal.error = null }

        viewModel.categories.observe(viewLifecycleOwner, ::renderCategories)
        viewModel.selectedCategoryId.observe(viewLifecycleOwner) { id ->
            categoryAdapter.selectedId = id
            if (id != null) binding.tvCategoryError.isVisible = false
        }
        viewModel.uiState.observe(viewLifecycleOwner, ::render)
    }

    private fun submit() {
        val name = binding.etName.text?.toString().orEmpty()
        val description = binding.etDescription.text?.toString().orEmpty()
        val goal = binding.etGoal.text?.toString().orEmpty()

        val nameError = HabitInputValidator.validateName(name)
        val goalError = HabitInputValidator.validateGoal(goal)
        val categoryError = HabitInputValidator.validateCategory(viewModel.selectedCategoryId.value)
        binding.tilName.error = nameError?.let(::getString)
        binding.tilGoal.error = goalError?.let(::getString)
        binding.tvCategoryError.isVisible = categoryError != null
        if (nameError != null || goalError != null || categoryError != null) return

        hideKeyboard()
        viewModel.createHabit(name, description, goal)
    }

    private fun renderCategories(state: CategoriesState) {
        binding.progressCategories.isVisible = state == CategoriesState.Loading
        binding.groupCategoriesError.isVisible = state is CategoriesState.Error
        binding.rvCategories.isVisible = state is CategoriesState.Loaded
        when (state) {
            is CategoriesState.Loaded -> categoryAdapter.submitList(state.categories)
            is CategoriesState.Error -> binding.tvCategoriesError.text =
                getString(R.string.error_with_reason, getString(R.string.categories_load_failed), state.message)
            CategoriesState.Loading -> Unit
        }
    }

    private fun render(state: AddHabitUiState) {
        setLoading(state == AddHabitUiState.Loading)
        when (state) {
            is AddHabitUiState.Success -> {
                viewModel.onStateHandled()
                openCreateSchedule(state.habitId)
                showAeroSnackbar(getString(R.string.habit_created))
            }
            is AddHabitUiState.Error -> {
                viewModel.onStateHandled()
                showAeroSnackbar(state.message, isError = true)
            }
            AddHabitUiState.SessionExpired -> navigateToLoginAfterSessionExpired()
            AddHabitUiState.Idle, AddHabitUiState.Loading -> Unit
        }
    }

    /** Replaces this form with CreateSchedule, so Back from there returns to Home. */
    private fun openCreateSchedule(habitId: Long?) {
        if (findNavController().currentDestination?.id != R.id.addHabitFragment) return
        findNavController().navigate(
            R.id.action_addHabitFragment_to_createScheduleFragment,
            bundleOf(CreateScheduleFragment.ARG_HABIT_ID to (habitId ?: CreateScheduleFragment.NO_HABIT))
        )
    }

    private fun setLoading(loading: Boolean) {
        binding.progressCreate.isVisible = loading
        binding.btnCreateHabit.isEnabled = !loading
        binding.btnCreateHabit.text = if (loading) null else getString(R.string.action_create_habit)
        binding.tilName.isEnabled = !loading
        binding.tilDescription.isEnabled = !loading
        binding.tilGoal.isEnabled = !loading
        binding.rvCategories.isEnabled = !loading
        binding.tvScheduleExisting.isEnabled = !loading
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
