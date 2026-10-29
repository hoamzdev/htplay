package com.htdev.movie.htplay.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.htdev.movie.htplay.databinding.ItemMovieBinding
import com.htdev.movie.htplay.entities.ItemMovie
import androidx.core.view.isVisible
import com.htdev.movie.htplay.util.loadPoster

class MovieAdapter(
    val onClick: (ItemMovie) -> Unit
) : ListAdapter<ItemMovie, MovieAdapter.MovieSectorViewHolder>(diffUtil) {
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): MovieSectorViewHolder {
        val inflate = LayoutInflater.from(parent.context)
        val binding: ItemMovieBinding = ItemMovieBinding.inflate(inflate, parent, false)
        return MovieSectorViewHolder(binding)
    }

    override fun onBindViewHolder(
        holder: MovieSectorViewHolder,
        position: Int
    ) {
        holder.bind(getItem(position))
    }

    inner class MovieSectorViewHolder(val binding: ItemMovieBinding) :
        RecyclerView.ViewHolder(binding.root) {
        init {
            binding.root.setOnClickListener {
                bindingAdapterPosition.takeIf { it != RecyclerView.NO_POSITION }
                    ?.let { onClick(getItem(it)) }
            }
        }

        fun bind(item: ItemMovie) {
            binding.apply {
                ratingText.text = item.rateCount.toString()
                bookTitle.text = item.nameFile
                freeBadge.text = item.tagFilm
                freeBadge.isVisible = !item.tagFilm.isNullOrBlank()
                bookCover.loadPoster(item.urlPoster)
                root.contentDescription = item.nameFile
            }
        }
    }

    companion object {
        val diffUtil = object : DiffUtil.ItemCallback<ItemMovie>() {
            override fun areItemsTheSame(
                oldItem: ItemMovie,
                newItem: ItemMovie
            ): Boolean {
                return newItem.id == oldItem.id
            }

            override fun areContentsTheSame(
                oldItem: ItemMovie,
                newItem: ItemMovie
            ): Boolean {
                return newItem == oldItem
            }
        }
    }
}
