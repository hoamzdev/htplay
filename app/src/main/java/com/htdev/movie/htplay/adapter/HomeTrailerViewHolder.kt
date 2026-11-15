package com.htdev.movie.htplay.adapter

import android.annotation.SuppressLint
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.htdev.movie.htplay.R
import com.htdev.movie.htplay.databinding.ItemHtRecommentBinding
import com.htdev.movie.htplay.entities.HomeItem
import com.htdev.movie.htplay.util.trailerHtml
import com.htdev.movie.htplay.widget.onClickListener

@SuppressLint("SetJavaScriptEnabled")
internal class HomeTrailerViewHolder(
    private val binding: ItemHtRecommentBinding,
    private val itemAt: (Int) -> HomeItem.TrailerSector?,
    onWatchMovie: (String) -> Unit,
    onFavoriteMovie: () -> Unit,
    onRetry: (HomeItem) -> Unit,
) : RecyclerView.ViewHolder(binding.root) {
    private var pendingHtml: String? = null
    private var loadedHtml: String? = null
    private var destroyed = false

    init {
        binding.wbTrailer.apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.allowFileAccess = false
            settings.allowContentAccess = false
            settings.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
            settings.mediaPlaybackRequiresUserGesture = false
            webChromeClient = WebChromeClient()
            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean =
                    request.isForMainFrame && request.url.scheme != "about"
            }
        }
        binding.btnWatchNow.onClickListener {
            currentItem()?.recommendation?.movie?.slug?.let(onWatchMovie)
        }
        binding.btnFavorite.onClickListener {
            if (currentItem()?.recommendation?.movie != null) onFavoriteMovie()
        }
        binding.tvTrailerStatus.onClickListener {
            currentItem()?.takeIf { it.recommendation.hasError }?.let(onRetry)
        }
    }

    private fun currentItem(): HomeItem.TrailerSector? =
        bindingAdapterPosition.takeIf { it != RecyclerView.NO_POSITION }?.let(itemAt)

    fun bind(item: HomeItem.TrailerSector, resumed: Boolean) = with(binding) {
        val recommendation = item.recommendation
        val movie = recommendation.movie
        pendingHtml = movie?.let { trailerHtml(it.trailerUrl, root.context.packageName) }
        tvTrailerStatus.isVisible = recommendation.isLoading || recommendation.hasError || pendingHtml == null
        tvTrailerStatus.setText(when {
            recommendation.isLoading -> R.string.loading
            recommendation.hasError -> R.string.home_load_error_retry
            else -> R.string.home_trailer_unavailable
        })
        tvTrailerStatus.isClickable = recommendation.hasError
        tvTrailerStatus.isFocusable = recommendation.hasError
        wbTrailer.isInvisible = tvTrailerStatus.isVisible
        wbTrailer.contentDescription = movie?.name
        btnWatchNow.isEnabled = movie != null
        btnFavorite.isEnabled = movie != null && recommendation.isFavorite != null && !recommendation.isUpdatingFavorite
        btnFavorite.setText(if (recommendation.isFavorite == true) R.string.home_remove_from_favorites else R.string.home_add_to_favorites)
        setActive(resumed && root.isAttachedToWindow)
    }

    fun setActive(active: Boolean) {
        if (destroyed) return
        val web = binding.wbTrailer
        if (active && pendingHtml != null) {
            web.onResume()
            if (loadedHtml != pendingHtml) {
                loadedHtml = pendingHtml
                // YouTube requires the embedding app's identity as the HTTPS referrer.
                // https://developers.google.com/youtube/terms/required-minimum-functionality#embedded-player-api-client-identity
                web.loadDataWithBaseURL("https://${web.context.packageName}/", pendingHtml!!, "text/html", "UTF-8", null)
            }
        } else {
            if (loadedHtml != null) {
                web.stopLoading()
                web.loadUrl("about:blank")
                loadedHtml = null
            }
            web.onPause()
        }
    }

    fun recycle() {
        pendingHtml = null
        setActive(false)
    }

    fun release() {
        if (destroyed) return
        recycle()
        val web = binding.wbTrailer
        (web.parent as? ViewGroup)?.removeView(web)
        web.destroy()
        destroyed = true
    }
}
