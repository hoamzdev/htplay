package com.htdev.movie.htplay.search

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.paging.PagingData
import androidx.paging.PagingDataEvent
import androidx.paging.PagingDataPresenter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.htdev.movie.htplay.databinding.ItemHotSearchBinding
import com.htdev.movie.htplay.util.loadPoster

class HotSearchAdapter(
    private val onClick: (SearchItem) -> Unit = {},
) : RecyclerView.Adapter<HotSearchAdapter.ViewHolder>() {
    private val presenter = object : PagingDataPresenter<SearchItem>() {
        override suspend fun presentPagingDataEvent(event: PagingDataEvent<SearchItem>) {
            when (event) {
                is PagingDataEvent.Append -> notifyItemRangeInserted(event.startIndex, event.inserted.size)
                is PagingDataEvent.Prepend -> notifyDataSetChanged()
                is PagingDataEvent.DropAppend -> notifyItemRangeRemoved(event.startIndex, event.dropCount)
                is PagingDataEvent.DropPrepend -> notifyDataSetChanged()
                is PagingDataEvent.Refresh -> notifyDataSetChanged()
            }
        }
    }

    var showRank = true
        set(value) {
            if (field == value) return
            field = value
            notifyItemRangeChanged(0, itemCount)
        }

    val loadStateFlow = presenter.loadStateFlow
    suspend fun submitData(data: PagingData<SearchItem>) = presenter.collectFrom(data)
    fun retry() = presenter.retry()

    override fun getItemCount() = presenter.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ViewHolder(
        ItemHotSearchBinding.inflate(LayoutInflater.from(parent.context), parent, false),
    )

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        // Reading through the presenter triggers Paging's next-page viewport hints.
        presenter[position]?.let { holder.bind(it, position) }
    }

    override fun onViewRecycled(holder: ViewHolder) {
        holder.clearImage()
        super.onViewRecycled(holder)
    }

    inner class ViewHolder(private val binding: ItemHotSearchBinding) : RecyclerView.ViewHolder(binding.root) {
        // Recycling can also run during Activity destruction, so resolve Glide while it is alive.
        private val imageRequests = Glide.with(binding.ivThumbnail)

        init {
            binding.root.setOnClickListener { onItemClick() }
            binding.btnPlay.setOnClickListener { onItemClick() }
        }

        private fun onItemClick() {
            bindingAdapterPosition.takeIf { it != RecyclerView.NO_POSITION }
                ?.let(presenter::peek)?.let(onClick)
        }

        fun bind(item: SearchItem, position: Int) {
            binding.ivThumbnail.loadPoster(item.thumbnail)
            binding.tvNameMovie.text = item.name
            binding.tvYearMovie.text = item.year?.toString().orEmpty()
            binding.tvYearMovie.isVisible = item.year != null
            binding.tvRank.text = (position + 1).toString()
            binding.cardNumber.isVisible = showRank
            binding.root.contentDescription = item.name
        }

        fun clearImage() {
            imageRequests.clear(binding.ivThumbnail)
        }
    }
}
