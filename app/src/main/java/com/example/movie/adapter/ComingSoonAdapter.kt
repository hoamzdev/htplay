package com.example.movie.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.movie.databinding.ItemComingsoonBinding
import com.example.movie.entities.ComingSoon
import com.example.movie.util.loadPoster

class ComingSoonAdapter(
    val onClickCard :(ComingSoon) -> Unit
) : ListAdapter<ComingSoon, ComingSoonAdapter.ComingSoonViewHolder>(diffUtil){
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ComingSoonViewHolder {
        val inflate = LayoutInflater.from(parent.context)
        val binding : ItemComingsoonBinding = ItemComingsoonBinding.inflate(inflate, parent, false)
        return ComingSoonViewHolder(binding)
    }

    override fun onBindViewHolder(
        holder: ComingSoonViewHolder,
        position: Int
    ) {
        holder.bind(getItem(position))
    }

    inner class ComingSoonViewHolder(val binding : ItemComingsoonBinding) : RecyclerView.ViewHolder(binding.root){

        init {
            binding.vCard.setOnClickListener {
                bindingAdapterPosition.takeIf { it != RecyclerView.NO_POSITION }
                    ?.let { onClickCard(getItem(it)) }
            }
        }

        fun bind(item : ComingSoon){
            binding.apply {
                tvDate.text = item.date
                tvName.text = item.name
                bookCover.loadPoster(item.thumbnail)
                vCard.contentDescription = item.name
            }
        }
    }

    companion object{
        val diffUtil = object : DiffUtil.ItemCallback<ComingSoon>(){
            override fun areItemsTheSame(
                oldItem: ComingSoon,
                newItem: ComingSoon
            ): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(
                oldItem: ComingSoon,
                newItem: ComingSoon
            ): Boolean {
                return oldItem == newItem
            }
        }
    }

}
