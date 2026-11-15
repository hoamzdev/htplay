package com.htdev.movie.htplay.adapter

import com.htdev.movie.htplay.widget.onClickListener

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.htdev.movie.htplay.databinding.ItemHomeCategoryBinding

data class HomeCategoryItem(val slug: String, val name: String)

class HomeCategoryAdapter(
    private val onClick: (String) -> Unit,
) : ListAdapter<HomeCategoryItem, HomeCategoryAdapter.CategoryViewHolder>(DIFF) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder =
        CategoryViewHolder(ItemHomeCategoryBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        holder.binding.root.text = getItem(position).name
    }

    inner class CategoryViewHolder(val binding: ItemHomeCategoryBinding) : RecyclerView.ViewHolder(binding.root) {
        init {
            binding.root.onClickListener {
                bindingAdapterPosition.takeIf { it != RecyclerView.NO_POSITION }
                    ?.let { onClick(getItem(it).slug) }
            }
        }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<HomeCategoryItem>() {
            override fun areItemsTheSame(oldItem: HomeCategoryItem, newItem: HomeCategoryItem) =
                oldItem.slug == newItem.slug

            override fun areContentsTheSame(oldItem: HomeCategoryItem, newItem: HomeCategoryItem) =
                oldItem == newItem
        }
    }
}
