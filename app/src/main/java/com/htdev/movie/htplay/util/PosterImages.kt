package com.htdev.movie.htplay.util

import android.widget.ImageView
import com.bumptech.glide.Glide
import com.htdev.movie.htplay.R

fun ImageView.loadPoster(source: String?) {
    Glide.with(this)
        .load(source?.takeIf { it.isNotBlank() })
        .placeholder(R.drawable.book_cover)
        .error(R.drawable.book_cover)
        .centerCrop()
        .into(this)
}
