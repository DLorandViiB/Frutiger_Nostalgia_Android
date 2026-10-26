package com.example.firstproject.ui.schedule

import android.os.Bundle
import android.text.format.DateFormat
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.firstproject.R
import com.example.firstproject.databinding.FragmentCreateScheduleBinding
import com.example.firstproject.model.RepeatPattern
import com.example.firstproject.ui.common.navigateToLoginAfterSessionExpired
import com.example.firstproject.ui.common.showAeroSnackbar
import com.example.firstproject.ui.home.HomeFragment
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.DateValidatorPointForward
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.textfield.TextInputLayout
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

class CreateScheduleFragment : Fragment() {
    private var _binding: FragmentCreateScheduleBinding? = null
    private val binding get() = _binding!!

    private val viewModel: CreateScheduleViewModel by viewModels()
    private val habitAdapter = HabitDropdownAdapter()

    /** Dropdown order; labels come from [patternLabel]. */
    private val patterns = RepeatPattern.entries

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCreateScheduleBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (savedInstanceState == null) {
            binding.etDuration.setText(DEFAULT_DURATION.toString())
            binding.etRepeatDays.setText(DEFAULT_REPEAT_DAYS.toString())
        }

        binding.actHabit.setAdapter(habitAdapter)
        binding.actHabit.setOnItemClickListener { _, _, position, _ ->
            viewModel.selectHabit(habitAdapter.getItem(position).id)
            binding.tilHabit.error = null
        }
        binding.actRepeat.setSimpleItems(patterns.map(::patternLabel).toTypedArray())
        binding.actRepeat.setOnItemClickListener { _, _, position, _ -> viewModel.selectPattern(patterns[position]) }

        binding.etDate.setOnClickListener { showDatePicker() }
        binding.tilDate.setEndIconOnClickListener { showDatePicker() }
        binding.etStartTime.setOnClickListener { showTimePicker() }
        binding.tilStartTime.setEndIconOnClickListener { showTimePicker() }
        binding.btnCreateSchedule.setOnClickListener { submit() }
        // Clear an inline error as soon as the user edits that field
        binding.etDuration.doAfterTextChanged { binding.tilDuration.error = null }
        binding.etRepeatDays.doAfterTextChanged { binding.tilRepeatDays.error = null }

        // Pickers survive rotation as dialogs, but their listeners don't: re-attach them
        (childFragmentManager.findFragmentByTag(TAG_DATE_PICKER) as? MaterialDatePicker<*>)
            ?.let(::attachDateListener)
        (childFragmentManager.findFragmentByTag(TAG_TIME_PICKER) as? MaterialTimePicker)
            ?.let(::attachTimeListener)

        viewModel.habits.observe(viewLifecycleOwner, ::renderHabits)
        viewModel.selectedHabitId.observe(viewLifecycleOwner) { renderSelectedHabit() }
        viewModel.pattern.observe(viewLifecycleOwner) { pattern ->
            binding.actRepeat.setText(patternLabel(pattern), false)
            val once = pattern == RepeatPattern.NONE
            binding.groupRepeatDays.isVisible = !once
            binding.tilDate.isEnabled = once
            binding.tilDate.helperText = if (once) null else getString(R.string.helper_date_repeating)
            renderDate()
        }
        viewModel.date.observe(viewLifecycleOwner) { renderDate() }
        viewModel.startTime.observe(viewLifecycleOwner) { time ->
            binding.etStartTime.setText(time.format(timeFormatter()))
            binding.tilStartTime.clearError()
        }
        viewModel.uiState.observe(viewLifecycleOwner, ::render)
    }

    private fun submit() {
        val pattern = viewModel.pattern.value ?: return
        val time = viewModel.startTime.value ?: return
        val date = viewModel.effectiveDate()
        val duration = binding.etDuration.text?.toString()?.trim()?.toIntOrNull()
        val repeatDays = binding.etRepeatDays.text?.toString()?.trim()?.toIntOrNull()

        val habitError = if (viewModel.selectedHabitId.value == null) R.string.error_required_habit else null
        val dateError = ScheduleInputValidator.validateDate(date, LocalDate.now())
        val durationError = ScheduleInputValidator.validateDuration(duration)
        val repeatDaysError = ScheduleInputValidator.validateRepeatDays(pattern, repeatDays)
        val utcWindow = ScheduleInputValidator.utcSafeWindowIfOutside(date, time, ZoneId.systemDefault())

        binding.tilHabit.error = habitError?.let(::getString)
        binding.tilDate.error = dateError?.let(::getString)
        binding.tilDuration.error = durationError?.let {
            getString(it, ScheduleInputValidator.DURATION_MIN, ScheduleInputValidator.DURATION_MAX)
        }
        binding.tilRepeatDays.error = repeatDaysError?.let {
            getString(it, ScheduleInputValidator.REPEAT_DAYS_MIN, ScheduleInputValidator.REPEAT_DAYS_MAX)
        }
        binding.tilStartTime.error = utcWindow?.let {
            getString(
                R.string.error_time_utc_window,
                it.start.format(timeFormatter()),
                it.endInclusive.format(timeFormatter())
            )
        }
        if (habitError != null || dateError != null || durationError != null ||
            repeatDaysError != null || utcWindow != null || duration == null
        ) return

        hideKeyboard()
        viewModel.createSchedule(
            durationMinutes = duration,
            repeatDays = repeatDays ?: DEFAULT_REPEAT_DAYS,
            notes = binding.etNotes.text?.toString().orEmpty()
        )
    }

    private fun renderHabits(state: HabitsState) {
        binding.tilHabit.isEnabled = state is HabitsState.Loaded && state.habits.isNotEmpty()
        binding.tilHabit.helperText =
            if (state == HabitsState.Loading) getString(R.string.loading) else null
        val loaded = state as? HabitsState.Loaded
        habitAdapter.habits = loaded?.habits.orEmpty()

        when {
            state is HabitsState.Error -> showHabitsMessage(
                getString(R.string.error_with_reason, getString(R.string.habits_load_failed), state.message),
                getString(R.string.action_retry)
            ) { viewModel.loadHabits() }
            loaded != null && loaded.habits.isEmpty() -> showHabitsMessage(
                getString(R.string.habits_empty), getString(R.string.action_create_one)
            ) {
                if (findNavController().currentDestination?.id == R.id.createScheduleFragment) {
                    findNavController().navigate(R.id.action_createScheduleFragment_to_addHabitFragment)
                }
            }
            else -> binding.groupHabitsMessage.isVisible = false
        }
        renderSelectedHabit()
    }

    private fun showHabitsMessage(message: String, action: String, onAction: () -> Unit) {
        binding.groupHabitsMessage.isVisible = true
        binding.tvHabitsMessage.text = message
        binding.btnHabitsAction.text = action
        binding.btnHabitsAction.setOnClickListener { onAction() }
    }

    private fun renderSelectedHabit() {
        val id = viewModel.selectedHabitId.value
        val habit = habitAdapter.habits.firstOrNull { it.id == id }
        binding.actHabit.setText(habit?.let { HabitDropdownAdapter.displayName(it, requireContext()) }, false)
    }

    private fun renderDate() {
        binding.etDate.setText(
            viewModel.effectiveDate().format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))
        )
        binding.tilDate.clearError()
    }

    /** setError(null) keeps the error line's space; picker fields drop it so the form doesn't keep a gap. */
    private fun TextInputLayout.clearError() {
        error = null
        isErrorEnabled = false
    }

    private fun render(state: CreateScheduleUiState) {
        setLoading(state == CreateScheduleUiState.Loading)
        when (state) {
            is CreateScheduleUiState.Success -> {
                viewModel.onStateHandled()
                returnHomeAndRefresh()
                showAeroSnackbar(
                    resources.getQuantityString(
                        R.plurals.schedule_created, state.sessions,
                        state.habitName ?: getString(R.string.schedule_untitled), state.sessions
                    )
                )
            }
            is CreateScheduleUiState.Error -> {
                viewModel.onStateHandled()
                showAeroSnackbar(state.message, isError = true)
            }
            CreateScheduleUiState.SessionExpired -> navigateToLoginAfterSessionExpired()
            CreateScheduleUiState.Idle, CreateScheduleUiState.Loading -> Unit
        }
    }

    /** Pops back to Home and flags it to reload, so the new schedule shows up right away. */
    private fun returnHomeAndRefresh() {
        val navController = findNavController()
        if (navController.currentDestination?.id != R.id.createScheduleFragment) return
        navController.getBackStackEntry(R.id.homeFragment).savedStateHandle[HomeFragment.KEY_SCHEDULES_CHANGED] = true
        navController.popBackStack(R.id.homeFragment, false)
    }

    private fun showDatePicker() {
        if (childFragmentManager.findFragmentByTag(TAG_DATE_PICKER) != null) return
        // MaterialDatePicker works in UTC-midnight millis
        val todayUtc = LocalDate.now().atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        val selected = (viewModel.date.value ?: LocalDate.now())
            .atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        val picker = MaterialDatePicker.Builder.datePicker()
            .setTitleText(R.string.pick_date_title)
            .setSelection(selected)
            .setCalendarConstraints(
                CalendarConstraints.Builder()
                    .setStart(todayUtc)
                    .setOpenAt(selected)
                    .setValidator(DateValidatorPointForward.from(todayUtc))
                    .build()
            )
            .build()
        attachDateListener(picker)
        picker.show(childFragmentManager, TAG_DATE_PICKER)
    }

    private fun attachDateListener(picker: MaterialDatePicker<*>) {
        picker.addOnPositiveButtonClickListener { selection ->
            val millis = selection as? Long ?: return@addOnPositiveButtonClickListener
            viewModel.selectDate(Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate())
        }
    }

    private fun showTimePicker() {
        if (childFragmentManager.findFragmentByTag(TAG_TIME_PICKER) != null) return
        val current = viewModel.startTime.value ?: LocalTime.now()
        val picker = MaterialTimePicker.Builder()
            .setTimeFormat(if (DateFormat.is24HourFormat(requireContext())) TimeFormat.CLOCK_24H else TimeFormat.CLOCK_12H)
            .setHour(current.hour)
            .setMinute(current.minute)
            .setTitleText(R.string.pick_time_title)
            .build()
        attachTimeListener(picker)
        picker.show(childFragmentManager, TAG_TIME_PICKER)
    }

    private fun attachTimeListener(picker: MaterialTimePicker) {
        picker.addOnPositiveButtonClickListener {
            viewModel.selectStartTime(LocalTime.of(picker.hour, picker.minute))
        }
    }

    private fun timeFormatter(): DateTimeFormatter =
        DateTimeFormatter.ofPattern(if (DateFormat.is24HourFormat(requireContext())) "HH:mm" else "h:mm a")

    private fun patternLabel(pattern: RepeatPattern): String = getString(
        when (pattern) {
            RepeatPattern.NONE -> R.string.repeat_none
            RepeatPattern.DAILY -> R.string.repeat_daily
            RepeatPattern.WEEKDAYS -> R.string.repeat_weekdays
            RepeatPattern.WEEKENDS -> R.string.repeat_weekends
        }
    )

    private fun setLoading(loading: Boolean) {
        binding.progressCreate.isVisible = loading
        binding.btnCreateSchedule.isEnabled = !loading
        binding.btnCreateSchedule.text = if (loading) null else getString(R.string.action_create_schedule)
        listOf(
            binding.tilHabit, binding.tilRepeat, binding.tilStartTime,
            binding.tilDuration, binding.tilRepeatDays, binding.tilNotes
        ).forEach { it.isEnabled = !loading }
        // The date field is only editable for one-off schedules
        binding.tilDate.isEnabled = !loading && viewModel.pattern.value == RepeatPattern.NONE
        if (!loading) binding.tilHabit.isEnabled = habitAdapter.habits.isNotEmpty()
    }

    private fun hideKeyboard() {
        WindowCompat.getInsetsController(requireActivity().window, binding.root)
            .hide(WindowInsetsCompat.Type.ime())
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        /** Nav argument (Long): the habit to preselect, or [NO_HABIT]. */
        const val ARG_HABIT_ID = "habitId"
        const val NO_HABIT = -1L

        private const val DEFAULT_DURATION = 30
        private const val DEFAULT_REPEAT_DAYS = 30
        private const val TAG_DATE_PICKER = "schedule_date_picker"
        private const val TAG_TIME_PICKER = "schedule_time_picker"
    }
}
