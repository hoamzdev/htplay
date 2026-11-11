package com.htdev.movie.htplay.home.favorite

import android.content.Context
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import androidx.activity.OnBackPressedCallback
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.htdev.movie.htplay.R
import com.htdev.movie.htplay.base.BaseFragment
import com.htdev.movie.htplay.databinding.FavoriteFragmentBinding
import com.htdev.movie.htplay.detail.DetailActivity
import com.htdev.movie.htplay.domain.common.Result
import com.htdev.movie.htplay.util.launchRepeatOnStarted
import com.htdev.movie.htplay.widget.gone
import com.htdev.movie.htplay.widget.onClickListener
import com.htdev.movie.htplay.widget.visible
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest


@AndroidEntryPoint
class FavoriteFragment : BaseFragment<FavoriteFragmentBinding>() {
    private val favoriteMovieViewModel: FavoriteMovieViewModel by viewModels()
    private val searchBackCallback = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() = closeSearch()
    }
    private val favoriteMovieAdapter: FavoriteMovieAdapter by lazy {
        FavoriteMovieAdapter(
            onClickMovie = { slug ->
                handleClickMovie(slug)
            },
            onDeleteFavorite = { slug ->
                handleDeleteFavorite(slug)
            }
        )
    }

    override fun createViewBinding(): FavoriteFragmentBinding {
        return FavoriteFragmentBinding.inflate(layoutInflater)
    }


    override fun initView() {
        binding.edtSearch.setText(favoriteMovieViewModel.uiState.value.query)
        binding.rcFavorites.apply {
            adapter = favoriteMovieAdapter
            setHasFixedSize(true)
        }
    }

    override fun bindViewModel() {
        launchRepeatOnStarted {
            favoriteMovieViewModel.uiState.collectLatest { state ->
                val favorites = (state.favorites as? Result.Success)?.data
                favoriteMovieAdapter.submitList(favorites.orEmpty())
                binding.apply {
                    pgLoading.isVisible = state.favorites is Result.Loading
                    rcFavorites.isVisible = !favorites.isNullOrEmpty()
                    lnEmptySearch.isVisible = favorites?.isEmpty() == true && state.query.isNotBlank()
                    llEmpty.isVisible = favorites?.isEmpty() == true && state.query.isBlank()
                    tvFavorites.isVisible = !state.isSearchOpen
                    edtSearch.isVisible = state.isSearchOpen
                }
                searchBackCallback.isEnabled = state.isSearchOpen
            }
        }
    }

    private fun handleDeleteFavorite(slug: String) {
        favoriteMovieViewModel.deleteFavorite(slug)
    }


    private fun handleClickMovie(slug: String) {
        dismissKeyboard()
        val intent = DetailActivity.createIntent(requireContext(), movieSlug = slug)
        startActivity(intent)
    }

    override fun handleClickEvent() {
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, searchBackCallback)
        binding.apply {
            ivSearch.onClickListener {
                if (favoriteMovieViewModel.isSearchOpen.value) {
                    dismissKeyboard()
                } else {
                    favoriteMovieViewModel.openSearch()
                    tvFavorites.gone()
                    edtSearch.visible()
                    edtSearch.requestFocus()
                    val input = edtSearch
                    input.post {
                        if (input.isAttachedToWindow && input.hasFocus()) {
                            (input.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
                                .showSoftInput(input, InputMethodManager.SHOW_IMPLICIT)
                        }
                    }
                }
            }
            logoApp.onClickListener {
                closeSearch()
            }
            edtSearch.doAfterTextChanged {
                favoriteMovieViewModel.setQuery(it?.toString().orEmpty())
            }
            edtSearch.setOnEditorActionListener { _, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    dismissKeyboard()
                    true
                } else false
            }
            btnWatchNow.onClickListener {
                findNavController().navigate(R.id.homeFragment)
            }
        }
    }

    private fun closeSearch() {
        dismissKeyboard()
        binding.edtSearch.text?.clear()
        favoriteMovieViewModel.closeSearch()
    }

    private fun dismissKeyboard() {
        (requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
            .hideSoftInputFromWindow(binding.edtSearch.windowToken, 0)
        binding.edtSearch.clearFocus()
    }

    override fun onDestroyView() {
        binding.rcFavorites.adapter = null
        super.onDestroyView()
    }
}
