package com.htdev.movie.htplay.search

import com.htdev.movie.htplay.widget.onClickListener

import android.content.Context
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.paging.LoadState
import com.htdev.movie.htplay.R
import com.htdev.movie.htplay.base.BaseActivity
import com.htdev.movie.htplay.databinding.ActivitySearchBinding
import com.htdev.movie.htplay.detail.DetailActivity
import com.htdev.movie.htplay.util.launchRepeatOnStarted
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SearchActivity : BaseActivity<ActivitySearchBinding>() {
    private val viewModel: SearchViewModel by viewModels()
    private val searchAdapter = HotSearchAdapter(onClick = { movie ->
        startActivity(DetailActivity.createIntent(this, movie.slug))
    })
    override fun createViewBinding(): ActivitySearchBinding {
        return ActivitySearchBinding.inflate(layoutInflater)
    }

    override fun initView() {
        binding.rvSearch.adapter = searchAdapter
        binding.rvSearch.itemAnimator = null
        binding.edtSearch.setText(viewModel.query.value)
        binding.edtSearch.setSelection(binding.edtSearch.length())
    }

    override fun handleClickEvent() {
        binding.icBack.onClickListener {
            finish()
        }
        binding.edtSearch.doAfterTextChanged { viewModel.setQuery(it?.toString().orEmpty()) }
        binding.icSearch.onClickListener { dismissKeyboard() }
        binding.edtSearch.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                dismissKeyboard()
                true
            } else false
        }
        binding.tvSearchStatus.onClickListener {
            if (viewModel.query.value.isBlank()) viewModel.retryLatest()
            else searchAdapter.retry()
        }
    }

    override fun observerData() {
        // Keep the same presenter while detail is on top, including its scroll position.
        lifecycleScope.launch {
            viewModel.movies.collectLatest { movies ->
                searchAdapter.submitData(movies)
            }
        }
        var displayedQuery: String? = null
        launchRepeatOnStarted {
            launch {
                viewModel.query.collect { query ->
                    if (displayedQuery == query) return@collect
                    displayedQuery = query
                    binding.tvSearch.setText(if (query.isBlank()) R.string.search_hot else R.string.search_results)
                    binding.lnEmptySearch.isVisible = false
                    searchAdapter.showRank = query.isBlank()
                    binding.rvSearch.scrollToPosition(0)
                }
            }
            launch {
                searchAdapter.loadStateFlow.collect { states ->
                    if (states == null) return@collect
                    val error = states.refresh is LoadState.Error || states.append is LoadState.Error
                    val loading = states.refresh is LoadState.Loading || states.append is LoadState.Loading
                    val empty = searchAdapter.itemCount == 0 && states.refresh is LoadState.NotLoading &&
                        states.append.endOfPaginationReached
                    binding.lnEmptySearch.isVisible = empty && !loading && !error && viewModel.query.value.isNotBlank()
                    binding.progressSearch.isVisible = loading && searchAdapter.itemCount == 0
                    val message = when {
                        error -> R.string.home_load_error_retry
                        loading && searchAdapter.itemCount > 0 -> R.string.loading
                        empty && viewModel.query.value.isBlank() -> R.string.home_no_movies
                        else -> null
                    }
                    binding.tvSearchStatus.isVisible = message != null
                    message?.let(binding.tvSearchStatus::setText)
                    binding.tvSearchStatus.isClickable = error
                    binding.tvSearchStatus.isFocusable = error
                }
            }
        }
    }

    private fun dismissKeyboard() {
        viewModel.setQuery(binding.edtSearch.text?.toString().orEmpty())
        (getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
            .hideSoftInputFromWindow(binding.edtSearch.windowToken, 0)
        binding.edtSearch.clearFocus()
    }

    override fun onDestroy() {
        binding.rvSearch.adapter = null
        super.onDestroy()
    }
}
