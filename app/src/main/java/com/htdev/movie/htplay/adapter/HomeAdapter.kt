package com.htdev.movie.htplay.adapter

import com.htdev.movie.htplay.widget.onClickListener
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.core.view.isVisible
import com.htdev.movie.htplay.R
import com.htdev.movie.htplay.databinding.ItemContinueWatchingSectorBinding
import com.htdev.movie.htplay.databinding.ItemMovieSectorBinding
import com.htdev.movie.htplay.entities.HomeItem
import com.htdev.movie.htplay.entities.HomeViewAllTarget
import com.htdev.movie.htplay.entities.ContinueWatching
import com.htdev.movie.htplay.entities.ItemMovie
import com.htdev.movie.htplay.domain.entities.Country

class HomeAdapter(
    private val onMovieClick: (ItemMovie) -> Unit = {},
    private val onContinueClick: (ContinueWatching) -> Unit = {},
    private val onViewAllClick: (target: HomeViewAllTarget) -> Unit = {},
    private val onCountryClick: (Country) -> Unit = {},
    private val onRetryClick: (HomeItem) -> Unit = {},
) : ListAdapter<HomeItem, RecyclerView.ViewHolder>(DIFF_UTIL) {

    private fun RecyclerView.setupRow(rowAdapter: RecyclerView.Adapter<*>) {
        layoutManager = LinearLayoutManager(context, RecyclerView.HORIZONTAL, false)
        adapter = rowAdapter
        isNestedScrollingEnabled = false
    }

    override fun getItemViewType(position: Int): Int {
        return when(getItem(position)){
            is HomeItem.ContinueWatchingSector -> TYPE_CONTINUE
            is HomeItem.CountrySector -> TYPE_BROWSE
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
            TYPE_BROWSE -> BrowseViewHolder(ItemMovieSectorBinding.inflate(inflate, parent, false))
            else -> {
                val binding = ItemMovieSectorBinding.inflate(inflate, parent, false)
                MovieViewHolder(binding)
            }
        }
    }

    inner class ContinueWatchingSectorViewHolder(private val binding: ItemContinueWatchingSectorBinding) : RecyclerView.ViewHolder(binding.root){
        private val rowAdapter = ContinueWatchingAdapter(onContinueClick)
        init {
            binding.rcContinueWatching.setupRow(rowAdapter)
            binding.viewAll.onClickListener {
                (getItemAtPosition() as? HomeItem.ContinueWatchingSector)?.viewAllTarget?.let(onViewAllClick)
            }
        }
        fun bind(item: HomeItem.ContinueWatchingSector){
            rowAdapter.submitList(item.movies)
            binding.status.isVisible = item.movies.isEmpty()
            binding.rcContinueWatching.isVisible = item.movies.isNotEmpty()
            binding.viewAll.isVisible = item.movies.isNotEmpty()
        }
    }

    inner class MovieViewHolder(val binding: ItemMovieSectorBinding) : RecyclerView.ViewHolder(binding.root){
        private val rowAdapter = MovieAdapter(onMovieClick)
        init {
            binding.rcContinueWatching.setupRow(rowAdapter)
            binding.viewAll.onClickListener {
                (getItemAtPosition() as? HomeItem.MovieSector)?.viewAllTarget?.let(onViewAllClick)
            }
            binding.status.onClickListener {
                (getItemAtPosition() as? HomeItem.MovieSector)
                    ?.takeIf { it.hasError }?.let(onRetryClick)
            }
        }
        fun bind(item: HomeItem.MovieSector){
            binding.tag.text = item.Genre
            rowAdapter.submitList(item.movies)
            binding.bindStatus(item.isLoading, item.hasError, item.movies.isEmpty(),
                if (item.id == HomeItem.SELECTED_FOR_YOU) R.string.home_no_recommendations else R.string.home_no_movies)
        }
    }

    inner class BrowseViewHolder(private val binding: ItemMovieSectorBinding) : RecyclerView.ViewHolder(binding.root) {
        private val rowAdapter = HomeCategoryAdapter { slug ->
            (getItemAtPosition() as? HomeItem.CountrySector)
                ?.countries?.firstOrNull { it.slug == slug }?.let(onCountryClick)
        }

        init {
            binding.rcContinueWatching.setupRow(rowAdapter)
            binding.viewAll.onClickListener { getItemAtPosition()?.viewAllTarget?.let(onViewAllClick) }
            binding.status.onClickListener {
                (getItemAtPosition() as? HomeItem.CountrySector)
                    ?.takeIf { it.hasError }?.let(onRetryClick)
            }
        }

        fun bind(item: HomeItem.CountrySector) {
            binding.tag.setText(R.string.home_countries)
            rowAdapter.submitList(item.countries.map { HomeCategoryItem(it.slug, it.name) })
            binding.bindStatus(item.isLoading, item.hasError, item.countries.isEmpty(), R.string.home_no_categories)
        }
    }

    private fun ItemMovieSectorBinding.bindStatus(
        isLoading: Boolean,
        hasError: Boolean,
        isEmpty: Boolean,
        emptyMessage: Int,
    ) {
        status.isVisible = isLoading || hasError || isEmpty
        status.setText(when {
            isLoading -> R.string.loading
            hasError -> R.string.home_load_error_retry
            else -> emptyMessage
        })
        status.isClickable = hasError
        status.isFocusable = hasError
        rcContinueWatching.isVisible = !isLoading && !hasError && !isEmpty
        viewAll.isVisible = !isLoading && !hasError && !isEmpty
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
            is HomeItem.MovieSector -> {
                (holder as MovieViewHolder).bind(item)
            }
            is HomeItem.CountrySector -> (holder as BrowseViewHolder).bind(item)
        }
    }

    companion object{
        private const val TYPE_CONTINUE = 0
        private const val TYPE_NORMAL = 2
        private const val TYPE_BROWSE = 3
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
