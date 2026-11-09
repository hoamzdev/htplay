package com.htdev.movie.htplay.detail

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isInvisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.htdev.movie.htplay.R
import com.htdev.movie.htplay.databinding.ItemEpisodeBinding
import com.htdev.movie.htplay.domain.entities.MoviePlayer

internal class EpisodeAdapter : ListAdapter<EpisodeRow, EpisodeAdapter.EpisodeViewHolder>(DIFF) {
    var onEpisodeClick: (MoviePlayer) -> Unit = {}

    fun submitEpisodes(episodes: List<MoviePlayer>) {
        submitList(episodes.toEpisodeRows())
    }

    var selectedEpisodeId: String? = null
        set(value) {
            if (field == value) return
            val previousId = field
            val previousIndex = currentList.indexOfFirst { row -> row.episodes.any { it.id == previousId } }
            field = value
            val selectedIndex = currentList.indexOfFirst { row -> row.episodes.any { it.id == value } }
            if (previousIndex >= 0) notifyItemChanged(previousIndex, SELECTION_CHANGED)
            if (selectedIndex >= 0 && selectedIndex != previousIndex) {
                notifyItemChanged(selectedIndex, SELECTION_CHANGED)
            }
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EpisodeViewHolder =
        EpisodeViewHolder(ItemEpisodeBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: EpisodeViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    override fun onBindViewHolder(holder: EpisodeViewHolder, position: Int, payloads: MutableList<Any>) {
        if (payloads.isNotEmpty() && payloads.all { it == SELECTION_CHANGED }) {
            holder.bindSelection(getItem(position))
        } else {
            onBindViewHolder(holder, position)
        }
    }

    inner class EpisodeViewHolder(private val binding: ItemEpisodeBinding) : RecyclerView.ViewHolder(binding.root) {
        private val buttons = listOf(
            binding.episode1, binding.episode2, binding.episode3,
            binding.episode4, binding.episode5, binding.episode6,
        )

        init {
            buttons.forEachIndexed { column, button ->
                button.setOnClickListener {
                    bindingAdapterPosition.takeIf { it != RecyclerView.NO_POSITION }?.let { position ->
                        getItem(position).episodes.getOrNull(column)
                            ?.takeIf { it.linkM3u8.isNotBlank() }?.let(onEpisodeClick)
                    }
                }
            }
        }

        fun bind(row: EpisodeRow) {
            buttons.forEachIndexed { column, button ->
                val episode = row.episodes.getOrNull(column)
                val number = row.firstEpisodeIndex + column + 1
                button.isInvisible = episode == null
                button.text = if (episode == null) "" else number.toString()
                button.contentDescription = if (episode == null) null else
                    button.context.getString(R.string.detail_episode_number, number)
                button.isEnabled = episode?.linkM3u8?.isNotBlank() == true
                button.alpha = if (button.isEnabled) 1f else 0.5f
            }
            bindSelection(row)
        }

        fun bindSelection(row: EpisodeRow) {
            buttons.forEachIndexed { column, button ->
                val episode = row.episodes.getOrNull(column)
                button.isSelected = episode != null && episode.id == selectedEpisodeId
            }
        }
    }

    private companion object {
        const val SELECTION_CHANGED = "selection"
        val DIFF = object : DiffUtil.ItemCallback<EpisodeRow>() {
            override fun areItemsTheSame(oldItem: EpisodeRow, newItem: EpisodeRow) =
                oldItem.firstEpisodeIndex == newItem.firstEpisodeIndex &&
                    oldItem.episodes.first().id == newItem.episodes.first().id
            override fun areContentsTheSame(oldItem: EpisodeRow, newItem: EpisodeRow) = oldItem == newItem
        }
    }
}
