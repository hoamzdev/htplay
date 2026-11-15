package com.htdev.movie.htplay.home.download

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.offline.Download
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.htdev.movie.htplay.R
import com.htdev.movie.htplay.databinding.ItemDownloadBinding
import com.htdev.movie.htplay.util.loadPoster
import com.htdev.movie.htplay.widget.onClickListener

// Capture progress values and the poster source so old diff items cannot change underneath us.
internal data class DownloadItem(
    val id: String,
    val metadata: DownloadMetadata,
    val state: Int,
    val bytesDownloaded: Long,
    val percentDownloaded: Int,
    val posterSource: String?,
)

@androidx.annotation.OptIn(UnstableApi::class)
internal class DownloadAdapter(
    private val onClickDownload: (String) -> Unit,
    private val onDeleteDownload: (String) -> Unit,
) : ListAdapter<DownloadItem, DownloadAdapter.DownloadViewHolder>(DIFF) {

    init {
        stateRestorationPolicy = RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DownloadViewHolder =
        DownloadViewHolder(ItemDownloadBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: DownloadViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class DownloadViewHolder(private val binding: ItemDownloadBinding) : RecyclerView.ViewHolder(binding.root) {
        init {
            binding.root.onClickListener {
                bindingAdapterPosition.takeIf { it != RecyclerView.NO_POSITION }
                    ?.let { onClickDownload(getItem(it).id) }
            }
            binding.ivDelete.onClickListener {
                bindingAdapterPosition.takeIf { it != RecyclerView.NO_POSITION }
                    ?.let { onDeleteDownload(getItem(it).id) }
            }
        }

        fun bind(item: DownloadItem) = with(binding) {
            val context = root.context
            ivPoster.loadPoster(item.posterSource)
            tvName.text = item.metadata.title
            tvEpisode.text = when (item.state) {
                Download.STATE_COMPLETED -> item.metadata.episodeNumber?.let {
                    context.getString(R.string.detail_episode_number, it)
                } ?: context.getString(R.string.download_completed)
                Download.STATE_FAILED -> context.getString(R.string.download_failed)
                Download.STATE_DOWNLOADING -> context.getString(R.string.download_progress, item.percentDownloaded)
                Download.STATE_REMOVING -> context.getString(R.string.download_removing)
                Download.STATE_STOPPED -> context.getString(R.string.download_stopped)
                else -> context.getString(R.string.download_queued)
            }
            tvSize.text = context.getString(R.string.download_size_mb, item.bytesDownloaded / 1048576.0)
        }
    }

    private companion object {
        val DIFF = object : DiffUtil.ItemCallback<DownloadItem>() {
            override fun areItemsTheSame(oldItem: DownloadItem, newItem: DownloadItem) = oldItem.id == newItem.id

            override fun areContentsTheSame(oldItem: DownloadItem, newItem: DownloadItem) = oldItem == newItem
        }
    }
}
