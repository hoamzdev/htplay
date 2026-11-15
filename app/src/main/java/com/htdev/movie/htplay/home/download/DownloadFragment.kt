package com.htdev.movie.htplay.home.download

import com.htdev.movie.htplay.base.BaseFragment
import com.htdev.movie.htplay.databinding.FragmentDownloadBinding
import android.content.Context
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import androidx.activity.OnBackPressedCallback
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadService
import com.htdev.movie.htplay.R
import com.htdev.movie.htplay.detail.DetailActivity
import com.htdev.movie.htplay.service.NotificationService
import com.htdev.movie.htplay.widget.onClickListener
import com.htdev.movie.htplay.widget.checkNetworkConnected
import com.htdev.movie.htplay.util.launchRepeatOnStarted
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.time.Duration.Companion.milliseconds


@androidx.annotation.OptIn(UnstableApi::class)
@AndroidEntryPoint
class DownloadFragment : BaseFragment<FragmentDownloadBinding>() {
    private val downloadViewModel: DownloadViewModel by viewModels()
    private var downloads: List<Download> = emptyList()
    private var hasLoadedDownloads = false
    private val searchBackCallback = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() = closeSearch()
    }
    private val downloadAdapter = DownloadAdapter(
        onClickDownload = { id ->
            dismissKeyboard()
            startActivity(DetailActivity.createDownloadIntent(requireContext(), id))
        },
        onDeleteDownload = { id ->
            DownloadService.sendRemoveDownload(requireContext(), NotificationService::class.java, id, false)
        },
    )

    override fun initView() {
        binding.rcDownload.adapter = downloadAdapter
        binding.rcDownload.itemAnimator = null
        binding.edtSearch.setText(downloadViewModel.query.value)
        renderSearch()
        renderDownloads()
        val manager = OfflineDownloads.manager(requireContext())
        runCatching { DownloadService.start(requireContext(), NotificationService::class.java) }
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                while (true) {
                    downloads = withContext(Dispatchers.IO) {
                        manager.downloadIndex.getDownloads().use { cursor ->
                            buildList { while (cursor.moveToNext()) add(cursor.download) }
                        }
                    }
                    hasLoadedDownloads = true
                    renderDownloads()
                    delay(1000.milliseconds)
                }
            }
        }
    }

    override fun bindViewModel() {
        launchRepeatOnStarted {
            combine(downloadViewModel.query, downloadViewModel.isSearchOpen) { query, open -> query to open }
                .collect {
                    renderSearch()
                    renderDownloads()
                }
        }
    }

    override fun handleClickEvent() {
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, searchBackCallback)
        binding.ivSearch.onClickListener {
            if (downloadViewModel.isSearchOpen.value) {
                dismissKeyboard()
            } else {
                downloadViewModel.openSearch()
                renderSearch()
                binding.edtSearch.requestFocus()
                val input = binding.edtSearch
                input.post {
                    if (input.isAttachedToWindow && input.hasFocus()) {
                        (input.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
                            .showSoftInput(input, InputMethodManager.SHOW_IMPLICIT)
                    }
                }
            }
        }
        binding.logoApp.onClickListener { closeSearch() }
        binding.edtSearch.doAfterTextChanged { downloadViewModel.setQuery(it?.toString().orEmpty()) }
        binding.edtSearch.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                dismissKeyboard()
                true
            } else false
        }
        binding.btnWatchNow.onClickListener {
            androidx.navigation.fragment.NavHostFragment.findNavController(this).navigate(R.id.homeFragment)
        }
    }

    private fun renderSearch() {
        val open = downloadViewModel.isSearchOpen.value
        binding.tvDownloads.isVisible = !open
        binding.edtSearch.isVisible = open
        searchBackCallback.isEnabled = open
    }

    private fun closeSearch() {
        dismissKeyboard()
        binding.edtSearch.text?.clear()
        downloadViewModel.closeSearch()
        renderSearch()
        renderDownloads()
    }

    private fun dismissKeyboard() {
        (requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
            .hideSoftInputFromWindow(binding.edtSearch.windowToken, 0)
        binding.edtSearch.clearFocus()
    }

    private fun renderDownloads() {
        val query = downloadViewModel.query.value
        val context = requireContext()
        val connected = checkNetworkConnected(context)
        val items = downloads.mapNotNull { download ->
            val metadata = DownloadMetadata.fromByteArray(download.request.data)
            val episodeLabel = metadata.episodeNumber?.let { number ->
                getString(R.string.detail_episode_number, number)
            }.orEmpty()
            if (!metadata.matchesSearch(query, episodeLabel)) return@mapNotNull null
            if (connected) OfflineDownloads.cachePoster(context, metadata.posterUrl)
            DownloadItem(
                id = download.request.id,
                metadata = metadata,
                state = download.state,
                bytesDownloaded = download.bytesDownloaded,
                percentDownloaded = download.percentDownloaded.coerceAtLeast(0f).toInt(),
                posterSource = OfflinePosters.source(context, metadata.posterUrl),
            )
        }
        downloadAdapter.submitList(items)
        binding.pgLoading.isVisible = !hasLoadedDownloads
        binding.llEmpty.isVisible = hasLoadedDownloads && items.isEmpty() && query.isBlank()
        binding.lnEmptySearch.isVisible = hasLoadedDownloads && items.isEmpty() && query.isNotBlank()
        binding.rcDownload.isVisible = items.isNotEmpty()
    }

    override fun onDestroyView() {
        binding.rcDownload.adapter = null
        super.onDestroyView()
    }
    override fun createViewBinding(): FragmentDownloadBinding {
        return FragmentDownloadBinding.inflate(layoutInflater)
    }



}
