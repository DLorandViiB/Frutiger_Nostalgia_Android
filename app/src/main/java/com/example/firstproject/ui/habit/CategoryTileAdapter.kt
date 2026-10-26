package com.example.firstproject.ui.habit

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.firstproject.R
import com.example.firstproject.databinding.ItemCategoryTileBinding
import com.example.firstproject.model.HabitCategory
import com.example.firstproject.ui.common.CategoryIcons

/** Grid of category orbs loaded from the API; exactly one can be selected. */
class CategoryTileAdapter(
    private val onSelected: (HabitCategory) -> Unit
) : ListAdapter<HabitCategory, CategoryTileAdapter.ViewHolder>(DiffCallback) {

    var selectedId: Long? = null
        set(value) {
            if (field == value) return
            val previous = field
            field = value
            currentList.forEachIndexed { index, category ->
                if (category.id == previous || category.id == value) notifyItemChanged(index)
            }
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
        ViewHolder(ItemCategoryTileBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val category = getItem(position)
        holder.bind(category, category.id == selectedId)
        holder.itemView.setOnClickListener { onSelected(category) }
    }

    class ViewHolder(private val binding: ItemCategoryTileBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(category: HabitCategory, selected: Boolean) {
            val name = category.name?.takeIf { it.isNotBlank() }
                ?: binding.root.context.getString(R.string.schedule_category_unknown)
            binding.ivOrb.setImageResource(CategoryIcons.orbFor(category.name))
            binding.tvName.text = name
            // isSelected drives bg_category_tile and is announced as "selected" by TalkBack
            binding.root.isSelected = selected
            binding.root.contentDescription = name
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<HabitCategory>() {
        override fun areItemsTheSame(oldItem: HabitCategory, newItem: HabitCategory) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: HabitCategory, newItem: HabitCategory) = oldItem == newItem
    }
}
