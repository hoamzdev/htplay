package com.htdev.movie.htplay.detail

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.htdev.movie.htplay.R
import com.htdev.movie.htplay.databinding.ItemEpisodeBinding
import com.htdev.movie.htplay.domain.entities.MoviePlayer

class EpisodeAdapter : ListAdapter<MoviePlayer, EpisodeAdapter.EpisodeViewHolder>(DIFF) {
    var onEpisodeClick: (MoviePlayer) -> Unit = {}

    var selectedEpisodeId: String? = null
        set(value) {
            if (field == value) return
            val previousIndex = currentList.indexOfFirst { it.id == field }
            field = value
            val selectedIndex = currentList.indexOfFirst { it.id == value }
            if (previousIndex >= 0) notifyItemChanged(previousIndex)
            if (selectedIndex >= 0) notifyItemChanged(selectedIndex)
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EpisodeViewHolder =
        EpisodeViewHolder(ItemEpisodeBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: EpisodeViewHolder, position: Int) {
        holder.bind(getItem(position), position)
    }

    inner class EpisodeViewHolder(private val binding: ItemEpisodeBinding) : RecyclerView.ViewHolder(binding.root) {
        init {
            binding.root.setOnClickListener {
                bindingAdapterPosition.takeIf { it != RecyclerView.NO_POSITION }?.let { position ->
                    getItem(position).takeIf { it.linkM3u8.isNotBlank() }?.let(onEpisodeClick)
                }
            }
        }

        @SuppressLint("SetTextI18n")
        fun bind(episode: MoviePlayer, position: Int) {
            val context = binding.root.context
            val selected = episode.id == selectedEpisodeId
            binding.tvEpisode.text = (position + 1).toString()
            binding.root.contentDescription = context.getString(R.string.detail_episode_number, position + 1)
            binding.root.isSelected = selected
            binding.root.isEnabled = episode.linkM3u8.isNotBlank()
            binding.root.alpha = if (binding.root.isEnabled) 1f else 0.5f
            binding.root.setCardBackgroundColor(context.getColor(
                if (selected) R.color.primary_color else android.R.color.transparent,
            ))
            binding.root.strokeColor = context.getColor(
                if (selected) R.color.secondary_color else R.color.primary_color,
            )
        }
    }

    private companion object {
        val DIFF = object : DiffUtil.ItemCallback<MoviePlayer>() {
            override fun areItemsTheSame(oldItem: MoviePlayer, newItem: MoviePlayer) = oldItem.id == newItem.id
            override fun areContentsTheSame(oldItem: MoviePlayer, newItem: MoviePlayer) = oldItem == newItem
        }
    }
}
