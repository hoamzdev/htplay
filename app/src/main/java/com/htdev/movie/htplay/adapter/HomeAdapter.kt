package com.htdev.movie.htplay.adapter

import com.htdev.movie.htplay.widget.onClickListener
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.LinearLayoutManager
import com.htdev.movie.htplay.databinding.ItemComingsoonSectorBinding
import com.htdev.movie.htplay.databinding.ItemContinueWatchingSectorBinding
import com.htdev.movie.htplay.databinding.ItemMovieSectorBinding
import com.htdev.movie.htplay.entities.HomeItem
import com.htdev.movie.htplay.entities.ComingSoon
import com.htdev.movie.htplay.entities.ContinueWatching
import com.htdev.movie.htplay.entities.ItemMovie

class HomeAdapter(
    private val onMovieClick: (ItemMovie) -> Unit = {},
    private val onContinueClick: (ContinueWatching) -> Unit = {},
    private val onComingSoonClick: (ComingSoon) -> Unit = {},
    private val onViewAllClick: (HomeItem) -> Unit = {},
) : ListAdapter<HomeItem, RecyclerView.ViewHolder>(DIFF_UTIL) {

    private fun RecyclerView.setupRow(rowAdapter: RecyclerView.Adapter<*>) {
        layoutManager = LinearLayoutManager(context, RecyclerView.HORIZONTAL, false)
        adapter = rowAdapter
        isNestedScrollingEnabled = false
    }

    override fun getItemViewType(position: Int): Int {
        return when(getItem(position)){
            is HomeItem.ContinueWatchingSector -> TYPE_CONTINUE
            is HomeItem.ComingSoonSector -> TYPE_COMING_SOON
            else -> TYPE_NORMAL
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): RecyclerView.ViewHolder {
        val inflate = LayoutInflater.from(parent.context)
        return when(viewType){
            TYPE_CONTINUE -> {
                val binding = ItemContinueWatchingSectorBinding.inflate(inflate, parent, false)
                ContinueWatchingSectorViewHolder(binding)
            }
            TYPE_COMING_SOON -> {
                val binding = ItemComingsoonSectorBinding.inflate(inflate, parent, false)
                ComingSoonSectorViewHolder(binding)
            }
            else -> {
                val binding = ItemMovieSectorBinding.inflate(inflate, parent, false)
                MovieViewHolder(binding)
            }
        }
    }

    inner class ContinueWatchingSectorViewHolder(binding: ItemContinueWatchingSectorBinding) : RecyclerView.ViewHolder(binding.root){
        private val rowAdapter = ContinueWatchingAdapter(onContinueClick)
        init {
            binding.rcContinueWatching.setupRow(rowAdapter)
            binding.viewAll.onClickListener {
                (getItemAtPosition() as? HomeItem.ContinueWatchingSector)?.let(onViewAllClick)
            }
        }
        fun bind(item: HomeItem.ContinueWatchingSector){
            rowAdapter.submitList(item.movies)
        }
    }

    inner class ComingSoonSectorViewHolder(binding: ItemComingsoonSectorBinding) : RecyclerView.ViewHolder(binding.root){
        private val rowAdapter = ComingSoonAdapter(onComingSoonClick)
        init {
            binding.rcComingSoon.setupRow(rowAdapter)
            binding.viewAll.onClickListener {
                (getItemAtPosition() as? HomeItem.ComingSoonSector)?.let(onViewAllClick)
            }
        }
        fun bind(item: HomeItem.ComingSoonSector){
            rowAdapter.submitList(item.movies)
        }
    }

    inner class MovieViewHolder(val binding: ItemMovieSectorBinding) : RecyclerView.ViewHolder(binding.root){
        private val rowAdapter = MovieAdapter(onMovieClick)
        init {
            binding.rcContinueWatching.setupRow(rowAdapter)
        }
        fun bind(item: HomeItem.MovieSector){
            binding.tag.text = item.genre
            rowAdapter.submitList(item.movies)
        }
    }

    private fun RecyclerView.ViewHolder.getItemAtPosition(): HomeItem? =
        bindingAdapterPosition.takeIf { it != RecyclerView.NO_POSITION }?.let { getItem(it) }

    override fun onBindViewHolder(
        holder: RecyclerView.ViewHolder,
        position: Int
    ) {
        when(val item = getItem(position)){
            is HomeItem.ContinueWatchingSector -> {
                (holder as ContinueWatchingSectorViewHolder).bind(item)
            }
            is HomeItem.ComingSoonSector -> {
                (holder as ComingSoonSectorViewHolder).bind(item)
            }
            is HomeItem.MovieSector -> {
                (holder as MovieViewHolder).bind(item)
            }
        }
    }

    companion object{
        private const val TYPE_CONTINUE = 0
        private const val TYPE_COMING_SOON = 1
        private const val TYPE_NORMAL = 2
        val DIFF_UTIL = object : DiffUtil.ItemCallback<HomeItem>(){
            override fun areItemsTheSame(
                oldItem: HomeItem,
                newItem: HomeItem
            ): Boolean {
                return oldItem::class == newItem::class && oldItem.id == newItem.id
            }

            override fun areContentsTheSame(
                oldItem: HomeItem,
                newItem: HomeItem
            ): Boolean {
                return oldItem == newItem
            }
        }
    }
}
