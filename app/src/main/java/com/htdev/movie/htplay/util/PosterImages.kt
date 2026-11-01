package com.htdev.movie.htplay.util

import android.widget.ImageView
import android.graphics.drawable.Drawable
import android.util.Log
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.htdev.movie.htplay.BuildConfig
import com.htdev.movie.htplay.R

fun ImageView.loadPoster(source: String?) {
    Glide.with(this)
        .load(source?.trim()?.takeIf { it.isNotBlank() })
        .placeholder(R.drawable.iv_placeholder)
        .error(R.drawable.iv_placeholder)
        .centerCrop()
        .timeout(15_000)
        .listener(object : RequestListener<Drawable> {
            override fun onLoadFailed(
                e: GlideException?,
                model: Any?,
                target: Target<Drawable>,
                isFirstResource: Boolean
            ): Boolean {
                if (BuildConfig.DEBUG) {
                    Log.e("PosterImages", "Poster load failed: $model", e)
                }
                return false
            }

            override fun onResourceReady(
                resource: Drawable,
                model: Any,
                target: Target<Drawable>?,
                dataSource: DataSource,
                isFirstResource: Boolean
            ): Boolean = false
        })
        .into(this)
}
