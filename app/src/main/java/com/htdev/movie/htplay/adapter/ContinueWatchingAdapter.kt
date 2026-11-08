package com.htdev.movie.htplay.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.htdev.movie.htplay.R
import com.htdev.movie.htplay.databinding.ItemContinuewatchingBinding
import com.htdev.movie.htplay.entities.ContinueWatching
import com.htdev.movie.htplay.util.loadPoster
import com.htdev.movie.htplay.util.playbackProgressPercent

class ContinueWatchingAdapter(
    val onClickCard: (ContinueWatching) -> Unit,
) : ListAdapter<ContinueWatching, ContinueWatchingAdapter.ContinueWatchingViewHolder>(diffUtil) {
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ContinueWatchingViewHolder {
        val inflate = LayoutInflater.from(parent.context)
        val binding: ItemContinuewatchingBinding =
            ItemContinuewatchingBinding.inflate(inflate, parent, false)
        return ContinueWatchingViewHolder(binding)
    }

    override fun onBindViewHolder(
        holder: ContinueWatchingViewHolder,
        position: Int
    ) {
        holder.bind(getItem(position))
    }

    inner class ContinueWatchingViewHolder(val binding: ItemContinuewatchingBinding) :
        RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                bindingAdapterPosition.takeIf { it != RecyclerView.NO_POSITION }
                    ?.let { onClickCard(getItem(it)) }
            }
        }

        fun bind(item: ContinueWatching) {
            binding.apply {
                pgContinue.progress = playbackProgressPercent(item.currentProgress, item.totalProgress)
                ivThumbnail.loadPoster(item.urlThumbnail)
                tvTime.text =
                    root.context.getString(
                        R.string.playback_progress,
                        formatTime(item.currentProgress ?: 0),
                        formatTime(item.totalProgress ?: 0),
                    )
            }
        }
    }

    companion object {
        val diffUtil = object : DiffUtil.ItemCallback<ContinueWatching>() {
            override fun areItemsTheSame(
                oldItem: ContinueWatching,
                newItem: ContinueWatching
            ): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(
                oldItem: ContinueWatching,
                newItem: ContinueWatching
            ): Boolean {
                return oldItem == newItem
            }
        }
    }

}

fun formatTime(seconds: Long): String {
    val value = seconds.coerceAtLeast(0)
    val hours = value / 3600
    val minutes = (value % 3600) / 60
    val secs = value % 60

    return if (hours > 0) {
        "%02d:%02d:%02d".format(hours, minutes, secs)
    } else {
        "%02d:%02d".format(minutes, secs)
    }
}
