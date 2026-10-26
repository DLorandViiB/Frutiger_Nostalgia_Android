package com.example.firstproject.ui.home

import android.text.format.DateFormat
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.firstproject.R
import com.example.firstproject.databinding.ItemHomeScheduleBinding
import com.example.firstproject.model.ScheduleResponse
import com.example.firstproject.model.ScheduleStatus
import com.example.firstproject.ui.common.CategoryIcons
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class HomeScheduleAdapter :
    ListAdapter<ScheduleResponse, HomeScheduleAdapter.ViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
        ViewHolder(ItemHomeScheduleBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(getItem(position))

    class ViewHolder(private val binding: ItemHomeScheduleBinding) :
        RecyclerView.ViewHolder(binding.root) {

        private val context = binding.root.context

        fun bind(item: ScheduleResponse) {
            val title = item.habit?.name?.takeIf { it.isNotBlank() }
                ?: context.getString(R.string.schedule_untitled)
            val categoryName = item.habit?.category?.name
            val time = formatTimeRange(item.startTime, item.effectiveEndTime)
            val status = ScheduleStatus.fromApi(item.status)
            val statusText = statusLabel(status, item.status)

            binding.ivCategory.setImageResource(CategoryIcons.orbFor(categoryName))
            binding.tvTitle.text = title
            binding.tvTime.text = time
            binding.tvCategory.text = categoryName?.takeIf { it.isNotBlank() }
                ?: context.getString(R.string.schedule_category_unknown)
            binding.tvNotes.text = item.notes?.trim()
            binding.tvNotes.isVisible = !item.notes.isNullOrBlank()
            binding.tvStatus.text = statusText
            binding.tvStatus.setBackgroundResource(
                when (status) {
                    ScheduleStatus.PLANNED -> R.drawable.bg_pill_planned
                    ScheduleStatus.COMPLETED -> R.drawable.bg_pill_completed
                    ScheduleStatus.SKIPPED, null -> R.drawable.bg_pill_muted
                }
            )
            binding.root.contentDescription = context.getString(
                R.string.cd_schedule_item, title, time, binding.tvCategory.text, statusText
            )
        }

        /** Converts the UTC instants to the device zone, honouring its 12/24-hour setting. */
        private fun formatTimeRange(start: Instant?, end: Instant?): String {
            if (start == null) return context.getString(R.string.schedule_time_unknown)
            val formatter = DateTimeFormatter.ofPattern(
                if (DateFormat.is24HourFormat(context)) "HH:mm" else "h:mm a"
            )
            val zone = ZoneId.systemDefault()
            val startText = start.atZone(zone).format(formatter)
            if (end == null) return startText
            return context.getString(R.string.schedule_time_range, startText, end.atZone(zone).format(formatter))
        }

        private fun statusLabel(status: ScheduleStatus?, raw: String?): String = when (status) {
            ScheduleStatus.PLANNED -> context.getString(R.string.status_planned)
            ScheduleStatus.COMPLETED -> context.getString(R.string.status_completed)
            ScheduleStatus.SKIPPED -> context.getString(R.string.status_skipped)
            null -> raw?.takeIf { it.isNotBlank() } ?: context.getString(R.string.status_unknown)
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<ScheduleResponse>() {
        override fun areItemsTheSame(oldItem: ScheduleResponse, newItem: ScheduleResponse) =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: ScheduleResponse, newItem: ScheduleResponse) =
            oldItem == newItem
    }
}
