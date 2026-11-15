package com.htdev.movie.htplay.home.download

import com.htdev.movie.htplay.base.BaseFragment
import com.htdev.movie.htplay.databinding.FragmentDownloadBinding
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadService
import androidx.recyclerview.widget.RecyclerView
import com.htdev.movie.htplay.R
import com.htdev.movie.htplay.databinding.ItemDownloadBinding
import com.htdev.movie.htplay.detail.DetailActivity
import com.htdev.movie.htplay.service.NotificationService
import com.htdev.movie.htplay.widget.onClickListener
import com.htdev.movie.htplay.widget.checkNetworkConnected
import com.htdev.movie.htplay.util.loadPoster
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.time.Duration.Companion.milliseconds


@androidx.annotation.OptIn(UnstableApi::class)
class DownloadFragment : BaseFragment<FragmentDownloadBinding>() {
    private var downloads: List<Download> = emptyList()
    private val downloadAdapter = object : RecyclerView.Adapter<DownloadHolder>() {
        var items: List<Download> = emptyList()
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            DownloadHolder(ItemDownloadBinding.inflate(layoutInflater, parent, false))
        override fun getItemCount() = items.size
        override fun onBindViewHolder(holder: DownloadHolder, position: Int) {
            val item = items[position]
            val metadata = DownloadMetadata.fromByteArray(item.request.data)
            if (checkNetworkConnected(requireContext())) {
                OfflineDownloads.cachePoster(requireContext(), metadata.posterUrl)
            }
            with(holder.row) {
                ivPoster.loadPoster(OfflinePosters.source(requireContext(), metadata.posterUrl))
                tvName.text = metadata.title
                tvEpisode.text = when (item.state) {
                    Download.STATE_COMPLETED -> metadata.episodeNumber?.let {
                        getString(R.string.detail_episode_number, it)
                    } ?: getString(R.string.download_completed)
                    Download.STATE_FAILED -> getString(R.string.download_failed)
                    Download.STATE_DOWNLOADING -> getString(
                        R.string.download_progress, item.percentDownloaded.coerceAtLeast(0f).toInt()
                    )
                    Download.STATE_REMOVING -> getString(R.string.download_removing)
                    Download.STATE_STOPPED -> getString(R.string.download_stopped)
                    else -> getString(R.string.download_queued)
                }
                tvSize.text = getString(R.string.download_size_mb, item.bytesDownloaded / 1048576.0)
                ivDelete.onClickListener {
                    DownloadService.sendRemoveDownload(requireContext(), NotificationService::class.java,
                        item.request.id, false)
                }
                root.onClickListener {
                    startActivity(DetailActivity.createDownloadIntent(requireContext(), item.request.id))
                }
            }
        }
    }

    private class DownloadHolder(val row: ItemDownloadBinding) : RecyclerView.ViewHolder(row.root)

    override fun initView() {
        binding.rcDownload.adapter = downloadAdapter
        binding.edtSearch.doAfterTextChanged { renderDownloads() }
        binding.ivSearch.onClickListener {
            binding.edtSearch.isVisible = !binding.edtSearch.isVisible
            binding.tvDownloads.isVisible = !binding.edtSearch.isVisible
            if (!binding.edtSearch.isVisible) binding.edtSearch.text?.clear()
        }
        binding.btnWatchNow.onClickListener {
            androidx.navigation.fragment.NavHostFragment.findNavController(this)
                .navigate(R.id.homeFragment)
        }
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
                    renderDownloads()
                    delay(1000.milliseconds)
                }
            }
        }
    }

    private fun renderDownloads() {
        val query = binding.edtSearch.text?.toString().orEmpty()
        downloadAdapter.items = downloads.filter {
            val metadata = DownloadMetadata.fromByteArray(it.request.data)
            metadata.title.contains(query, true) || metadata.episodeNumber?.let { number ->
                getString(R.string.detail_episode_number, number).contains(query, true)
            } == true
        }
        downloadAdapter.notifyDataSetChanged()
        binding.llEmpty.isVisible = downloads.isEmpty()
        binding.lnEmptySearch.isVisible = downloads.isNotEmpty() && downloadAdapter.items.isEmpty()
        binding.rcDownload.isVisible = downloadAdapter.items.isNotEmpty()
    }

    override fun onDestroyView() {
        binding.rcDownload.adapter = null
        super.onDestroyView()
    }
    override fun createViewBinding(): FragmentDownloadBinding {
        return FragmentDownloadBinding.inflate(layoutInflater)
    }



}
