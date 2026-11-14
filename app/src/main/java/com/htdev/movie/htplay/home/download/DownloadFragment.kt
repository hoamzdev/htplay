package com.htdev.movie.htplay.home.download

import com.htdev.movie.htplay.base.BaseFragment
import com.htdev.movie.htplay.databinding.FragmentDownloadBinding
import android.content.Intent
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
import com.htdev.movie.htplay.databinding.ItemDownloadBinding
import com.htdev.movie.htplay.service.NotificationService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


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
            with(holder.row) {
                tvName.text = item.request.data.toString(Charsets.UTF_8)
                tvEpisode.text = when (item.state) {
                    Download.STATE_COMPLETED -> "Đã tải xong · Chạm để xem"
                    Download.STATE_FAILED -> "Tải thất bại · Chạm để thử lại"
                    Download.STATE_DOWNLOADING -> "Đang tải ${item.percentDownloaded.coerceAtLeast(0f).toInt()}%"
                    Download.STATE_REMOVING -> "Đang xóa"
                    Download.STATE_STOPPED -> "Đã dừng"
                    else -> "Đang chờ tải"
                }
                tvSize.text = "%.1f MB".format(item.bytesDownloaded / 1048576.0)
                ivDelete.contentDescription = "Xóa bản tải xuống"
                ivDelete.setOnClickListener {
                    DownloadService.sendRemoveDownload(requireContext(), NotificationService::class.java,
                        item.request.id, false)
                }
                root.setOnClickListener {
                    when (item.state) {
                        Download.STATE_COMPLETED -> startActivity(Intent(requireContext(), OfflinePlayerActivity::class.java)
                            .putExtra("download_id", item.request.id))
                        Download.STATE_FAILED, Download.STATE_STOPPED -> {
                            DownloadService.sendAddDownload(requireContext(), NotificationService::class.java, item.request, false)
                            DownloadService.sendResumeDownloads(requireContext(), NotificationService::class.java, false)
                        }
                        else -> Unit
                    }
                }
            }
        }
    }

    private class DownloadHolder(val row: ItemDownloadBinding) : RecyclerView.ViewHolder(row.root)

    override fun initView() {
        binding.rcDownload.adapter = downloadAdapter
        binding.edtSearch.doAfterTextChanged { renderDownloads() }
        binding.ivSearch.setOnClickListener {
            binding.edtSearch.isVisible = !binding.edtSearch.isVisible
            binding.tvDownloads.isVisible = !binding.edtSearch.isVisible
            if (!binding.edtSearch.isVisible) binding.edtSearch.text?.clear()
        }
        binding.btnWatchNow.setOnClickListener {
            androidx.navigation.fragment.NavHostFragment.findNavController(this)
                .navigate(com.htdev.movie.htplay.R.id.homeFragment)
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
                    delay(1000)
                }
            }
        }
    }

    private fun renderDownloads() {
        val query = binding.edtSearch.text?.toString().orEmpty()
        downloadAdapter.items = downloads.filter { it.request.data.toString(Charsets.UTF_8).contains(query, true) }
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
