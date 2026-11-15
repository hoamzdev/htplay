package com.htdev.movie.htplay.adapter

import com.htdev.movie.htplay.widget.onClickListener

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.htdev.movie.htplay.databinding.ItemShowAllCountryBinding
import com.htdev.movie.htplay.domain.entities.Country

class ShowAllCountryAdapter(
    private val onClick: (Country) -> Unit,
) : ListAdapter<Country, ShowAllCountryAdapter.ViewHolder>(DIFF) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ViewHolder(
        ItemShowAllCountryBinding.inflate(LayoutInflater.from(parent.context), parent, false),
    )

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.binding.countryName.text = getItem(position).name
    }

    inner class ViewHolder(val binding: ItemShowAllCountryBinding) : RecyclerView.ViewHolder(binding.root) {
        init {
            binding.countryName.onClickListener {
                bindingAdapterPosition.takeIf { it != RecyclerView.NO_POSITION }
                    ?.let { onClick(getItem(it)) }
            }
        }
    }

    private companion object {
        val DIFF = object : DiffUtil.ItemCallback<Country>() {
            override fun areItemsTheSame(oldItem: Country, newItem: Country) = oldItem.slug == newItem.slug
            override fun areContentsTheSame(oldItem: Country, newItem: Country) = oldItem == newItem
        }
    }
}
