package com.example.firstproject.ui.home

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.firstproject.R
import com.example.firstproject.databinding.FragmentHomeBinding
import com.example.firstproject.model.ScheduleStatus
import com.example.firstproject.ui.common.navigateToLoginAfterSessionExpired
import com.example.firstproject.utils.SessionManager
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

class HomeFragment : Fragment() {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: HomeViewModel by viewModels()
    private val adapter = HomeScheduleAdapter()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.rvSchedules.adapter = adapter

        binding.swipeRefresh.setColorSchemeResources(R.color.aero_sky, R.color.aero_aqua, R.color.aero_grass)
        binding.swipeRefresh.setOnRefreshListener { viewModel.refresh() }
        // The direct child is a FrameLayout, so tell the layout when the list itself can still scroll up
        binding.swipeRefresh.setOnChildScrollUpCallback { _, _ ->
            binding.rvSchedules.isVisible && binding.rvSchedules.canScrollVertically(-1)
        }
        binding.btnRetry.setOnClickListener { viewModel.retry() }
        binding.fabAddHabit.setOnClickListener {
            if (findNavController().currentDestination?.id == R.id.homeFragment) {
                findNavController().navigate(R.id.action_homeFragment_to_addHabitFragment)
            }
        }

        // CreateScheduleFragment sets this flag on Home's back stack entry before popping back here
        val homeEntryState = findNavController().getBackStackEntry(R.id.homeFragment).savedStateHandle
        homeEntryState.getLiveData(KEY_SCHEDULES_CHANGED, false).observe(viewLifecycleOwner) { changed ->
            if (changed) {
                homeEntryState[KEY_SCHEDULES_CHANGED] = false
                viewModel.refresh()
            }
        }

        viewModel.day.observe(viewLifecycleOwner, ::renderHeader)
        viewModel.isRefreshing.observe(viewLifecycleOwner) { binding.swipeRefresh.isRefreshing = it }
        viewModel.uiState.observe(viewLifecycleOwner, ::render)
    }

    override fun onResume() {
        super.onResume()
        viewModel.reloadIfDayChanged()
    }

    private fun renderHeader(day: LocalDate) {
        val greeting = getString(
            when (LocalTime.now().hour) {
                in 5..11 -> R.string.home_greeting_morning
                in 12..17 -> R.string.home_greeting_afternoon
                else -> R.string.home_greeting_evening
            }
        )
        val session = SessionManager(requireContext())
        val name = session.fetchUsername()?.takeIf { it.isNotBlank() }
            ?: session.fetchEmail()?.substringBefore('@')?.takeIf { it.isNotBlank() }
        binding.tvGreeting.text = getString(
            if (name != null) R.string.home_greeting_with_name else R.string.home_greeting_without_name,
            greeting
        )
        binding.tvName.text = name
        binding.tvName.isVisible = name != null
        binding.tvDate.text = day.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL))
    }

    private fun render(state: HomeUiState) {
        binding.progressSchedules.isVisible = state == HomeUiState.Loading
        binding.rvSchedules.isVisible = state is HomeUiState.Success && state.schedules.isNotEmpty()
        binding.cardEmpty.isVisible = state is HomeUiState.Success && state.schedules.isEmpty()
        binding.cardError.isVisible = state is HomeUiState.Error
        binding.tvSummary.isVisible = state is HomeUiState.Success && state.schedules.isNotEmpty()

        when (state) {
            is HomeUiState.Success -> {
                adapter.submitList(state.schedules)
                val done = state.schedules.count {
                    ScheduleStatus.fromApi(it.status) == ScheduleStatus.COMPLETED
                }
                binding.tvSummary.text = resources.getQuantityString(
                    R.plurals.home_summary, state.schedules.size, state.schedules.size, done
                )
            }
            is HomeUiState.Error -> binding.tvErrorMessage.text = state.message
            HomeUiState.SessionExpired -> navigateToLoginAfterSessionExpired()
            HomeUiState.Loading -> Unit
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        /** Boolean on Home's back stack entry: true when a schedule was just created elsewhere. */
        const val KEY_SCHEDULES_CHANGED = "schedules_changed"
    }
}
