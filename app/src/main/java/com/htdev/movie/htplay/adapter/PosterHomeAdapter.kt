package com.htdev.movie.htplay.adapter

import com.htdev.movie.htplay.widget.onClickListener

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.htdev.movie.htplay.R
import com.htdev.movie.htplay.databinding.ItemPosterHomeBinding
import com.htdev.movie.htplay.domain.entities.PosterMovieHome
import com.htdev.movie.htplay.util.getGenreName
import com.htdev.movie.htplay.util.loadPoster

/** Item cho banner Home; co the gan truc tiep vao ViewPager2. */
class PosterHomeAdapter(
    private val onMovieClick: (PosterMovieHome) -> Unit = {},
) : ListAdapter<PosterMovieHome, PosterHomeAdapter.PosterViewHolder>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PosterViewHolder =
        PosterViewHolder(ItemPosterHomeBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: PosterViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class PosterViewHolder(private val binding: ItemPosterHomeBinding) : RecyclerView.ViewHolder(binding.root) {

        val context = binding.root.context
        init {
            binding.root.onClickListener { currentMovie()?.let(onMovieClick) }
            binding.btnPlay.onClickListener { currentMovie()?.let(onMovieClick) }
        }

        private fun currentMovie(): PosterMovieHome? =
            bindingAdapterPosition.takeIf { it != RecyclerView.NO_POSITION }?.let { getItem(it) }

        fun bind(movie: PosterMovieHome) = with(binding) {
            imgPoster.loadPoster(movie.posterUrl)
            tvName.text = movie.name

            tvQuality.text = movie.quality
            tvQuality.isVisible = movie.quality.isNotBlank()
            tvGenre.text = context.getGenreName(movie.genre)
            tvGenre.isVisible = movie.genre.isNotBlank()
            metadata.isVisible = tvQuality.isVisible || tvGenre.isVisible

            tvVote.text = movie.vote
            tvVote.isVisible = movie.vote.isNotBlank()
            tvVote.contentDescription = root.context.getString(R.string.poster_home_vote, movie.vote)
            tvYear.text = movie.year.toString()
            tvYear.isVisible = movie.year > 0
            metadataDivider.isVisible = tvVote.isVisible && tvYear.isVisible
            ratingInfo.isVisible = tvVote.isVisible || tvYear.isVisible

            btnPlay.contentDescription = root.context.getString(R.string.poster_home_play_movie, movie.name)
        }

    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<PosterMovieHome>() {
            override fun areItemsTheSame(oldItem: PosterMovieHome, newItem: PosterMovieHome): Boolean =
                oldItem.id.ifBlank { oldItem.slug } == newItem.id.ifBlank { newItem.slug }

            override fun areContentsTheSame(oldItem: PosterMovieHome, newItem: PosterMovieHome): Boolean =
                oldItem == newItem
        }
    }
}
