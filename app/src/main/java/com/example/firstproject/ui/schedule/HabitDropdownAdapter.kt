package com.example.firstproject.ui.schedule

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.Filter
import android.widget.Filterable
import com.example.firstproject.R
import com.example.firstproject.databinding.ItemHabitDropdownBinding
import com.example.firstproject.model.HabitResponse
import com.example.firstproject.ui.common.CategoryIcons

/**
 * Rows for the habit picker: category orb, habit name and category. The field is read-only, so the
 * filter always returns every habit (ArrayAdapter's default filter would narrow the list to the
 * selected name, e.g. after rotation).
 */
class HabitDropdownAdapter : BaseAdapter(), Filterable {

    var habits: List<HabitResponse> = emptyList()
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    override fun getCount() = habits.size
    override fun getItem(position: Int): HabitResponse = habits[position]
    override fun getItemId(position: Int) = habits[position].id

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val binding = convertView?.let(ItemHabitDropdownBinding::bind)
            ?: ItemHabitDropdownBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        val habit = getItem(position)
        val context = parent.context
        binding.ivOrb.setImageResource(CategoryIcons.orbFor(habit.category?.name))
        binding.tvName.text = displayName(habit, context)
        binding.tvCategory.text = habit.category?.name?.takeIf { it.isNotBlank() }
            ?: context.getString(R.string.schedule_category_unknown)
        return binding.root
    }

    override fun getFilter(): Filter = object : Filter() {
        override fun performFiltering(constraint: CharSequence?) =
            FilterResults().apply { values = habits; count = habits.size }

        override fun publishResults(constraint: CharSequence?, results: FilterResults?) = notifyDataSetChanged()

        override fun convertResultToString(resultValue: Any?): CharSequence =
            (resultValue as? HabitResponse)?.name.orEmpty()
    }

    companion object {
        fun displayName(habit: HabitResponse, context: android.content.Context): String =
            habit.name?.takeIf { it.isNotBlank() } ?: context.getString(R.string.schedule_untitled)
    }
}
