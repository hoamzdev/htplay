package com.htdev.movie.htplay.widget

import androidx.core.view.isVisible
import com.google.android.material.tabs.TabLayout
import com.htdev.movie.htplay.R
import com.htdev.movie.htplay.databinding.ItemHomeGenreTabsBinding
import com.htdev.movie.htplay.domain.common.Result
import com.htdev.movie.htplay.domain.entities.Genre

class HomeGenreTabsController(
    private val binding: ItemHomeGenreTabsBinding,
    private val onSelect: (String?) -> Unit,
    private val onRetry: () -> Unit,
    private val includePopular: Boolean = true,
) {
    private var genres: Result<List<Genre>> = Result.Loading
    private var selectedSlug: String? = null

    fun submit(genres: Result<List<Genre>>, selectedSlug: String?) {
        this.genres = genres
        this.selectedSlug = selectedSlug
        bind()
    }

    private var bindingTabs = false
    private var displayedGenres: List<Genre>? = null

    init {
        binding.tabs.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                if (!bindingTabs) onSelect(tab.tag as? String)
            }
            override fun onTabUnselected(tab: TabLayout.Tab) = Unit
            override fun onTabReselected(tab: TabLayout.Tab) = Unit
        })
        binding.status.onClickListener { if (genres is Result.Error) onRetry() }
    }

    private fun bind() {
        val items = (genres as? Result.Success)?.data.orEmpty()
        bindingTabs = true
        if (displayedGenres != items) {
            binding.tabs.removeAllTabs()
            if (includePopular) {
                binding.tabs.addTab(binding.tabs.newTab().setText(R.string.home_popular))
            }
            items.forEach { genre ->
                binding.tabs.addTab(binding.tabs.newTab().setText(genre.name).setTag(genre.slug))
            }
            displayedGenres = items
        }
        val selected = items.indexOfFirst { it.slug == selectedSlug } + if (includePopular) 1 else 0
        if (binding.tabs.selectedTabPosition != selected) {
            binding.tabs.getTabAt(selected)?.select()
        }
        bindingTabs = false
        binding.status.isVisible = genres is Result.Loading || genres is Result.Error
        binding.status.setText(if (genres is Result.Error) R.string.home_load_error_retry else R.string.loading)
        binding.status.isClickable = genres is Result.Error
        binding.status.isFocusable = genres is Result.Error
    }
}
