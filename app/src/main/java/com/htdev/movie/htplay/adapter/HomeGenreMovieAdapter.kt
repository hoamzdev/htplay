package com.htdev.movie.htplay.adapter

import com.htdev.movie.htplay.widget.onClickListener

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.paging.PagingData
import androidx.paging.PagingDataEvent
import androidx.paging.PagingDataPresenter
import com.htdev.movie.htplay.databinding.ItemMovieBinding
import com.htdev.movie.htplay.domain.entities.PosterMovie
import com.htdev.movie.htplay.util.loadPoster

/** Presents the existing domain pager directly, with placeholders disabled by the repository. */
class HomeGenreMovieAdapter(
    private val onClick: (PosterMovie) -> Unit,
) : RecyclerView.Adapter<HomeGenreMovieAdapter.ViewHolder>() {
    private val presenter = object : PagingDataPresenter<PosterMovie>() {
        override suspend fun presentPagingDataEvent(event: PagingDataEvent<PosterMovie>) {
            when (event) {
                is PagingDataEvent.Append -> notifyItemRangeInserted(event.startIndex, event.inserted.size)
                is PagingDataEvent.Prepend -> notifyItemRangeInserted(0, event.inserted.size)
                is PagingDataEvent.DropAppend -> notifyItemRangeRemoved(event.startIndex, event.dropCount)
                is PagingDataEvent.DropPrepend -> notifyItemRangeRemoved(0, event.dropCount)
                is PagingDataEvent.Refresh -> notifyDataSetChanged()
            }
        }
    }
    val loadStateFlow = presenter.loadStateFlow
    suspend fun submitData(data: PagingData<PosterMovie>) = presenter.collectFrom(data)
    fun retry() = presenter.retry()
    override fun getItemCount() = presenter.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemMovieBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        binding.root.layoutParams = (binding.root.layoutParams as ViewGroup.MarginLayoutParams).apply {
            width = ViewGroup.LayoutParams.MATCH_PARENT
            val gap = (6 * parent.resources.displayMetrics.density).toInt()
            setMargins(gap, gap, gap, gap)
            marginEnd = gap
        }
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        // Reading through the presenter supplies Paging's viewport hints for the next page.
        presenter[position]?.let(holder::bind)
    }

    inner class ViewHolder(private val binding: ItemMovieBinding) : RecyclerView.ViewHolder(binding.root) {
        init {
            binding.root.onClickListener {
                bindingAdapterPosition.takeIf { it != RecyclerView.NO_POSITION }
                    ?.let(presenter::peek)?.let(onClick)
            }
        }
        fun bind(movie: PosterMovie) {
            binding.bookTitle.text = movie.name
            binding.bookCover.loadPoster(movie.posterUrl)
            binding.root.contentDescription = movie.name
        }
    }
}
