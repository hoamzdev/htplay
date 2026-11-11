package com.htdev.movie.htplay.home.favorite

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.htdev.movie.htplay.databinding.ItemFavoriteBinding
import com.htdev.movie.htplay.domain.entities.Favorite
import com.htdev.movie.htplay.util.loadPoster
import com.htdev.movie.htplay.widget.onClickListener

class FavoriteMovieAdapter(
    val onClickMovie: (String) -> Unit,
    val onDeleteFavorite: (String) -> Unit
) :
    ListAdapter<Favorite, FavoriteMovieAdapter.FavoriteMovieViewHolder>(diffUtil) {

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): FavoriteMovieViewHolder {
        val inflate = LayoutInflater.from(parent.context)
        return FavoriteMovieViewHolder(ItemFavoriteBinding.inflate(inflate, parent, false))
    }

    override fun onBindViewHolder(
        holder: FavoriteMovieViewHolder,
        position: Int
    ) {
        holder.bind(getItem(position))
    }

    inner class FavoriteMovieViewHolder(val binding: ItemFavoriteBinding) :
        RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.onClickListener {
                val position = bindingAdapterPosition
                if(position == RecyclerView.NO_POSITION) return@onClickListener
                val item = getItem(position)
                onClickMovie.invoke(item.slugMovie)
            }
            binding.ivCheckedFavorite.onClickListener {
                val position = bindingAdapterPosition
                if(position == RecyclerView.NO_POSITION) return@onClickListener
                val item = getItem(position)
                onDeleteFavorite.invoke(item.slugMovie)
            }
        }

        fun bind(item: Favorite) {
            binding.apply {
                bookCover.loadPoster(item.posterUrl)
                bookTitle.text = item.name
            }
        }
    }

    companion object {
        val diffUtil = object : DiffUtil.ItemCallback<Favorite>() {
            override fun areItemsTheSame(
                oldItem: Favorite,
                newItem: Favorite
            ) = newItem.slugMovie == oldItem.slugMovie

            override fun areContentsTheSame(
                oldItem: Favorite,
                newItem: Favorite
            ) = newItem == oldItem
        }
    }
}
