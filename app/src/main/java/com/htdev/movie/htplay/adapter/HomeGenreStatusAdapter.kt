package com.htdev.movie.htplay.adapter

import com.htdev.movie.htplay.widget.onClickListener

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.htdev.movie.htplay.R

class HomeGenreStatusAdapter(private val onRetry: () -> Unit) : RecyclerView.Adapter<HomeGenreStatusAdapter.ViewHolder>() {
    private var message: Int? = null
    private var canRetry = false

    fun submit(message: Int?, canRetry: Boolean = false) {
        val wasVisible = this.message != null
        if (this.message == message && this.canRetry == canRetry) return
        this.message = message
        this.canRetry = canRetry
        when {
            !wasVisible && message != null -> notifyItemInserted(0)
            wasVisible && message == null -> notifyItemRemoved(0)
            message != null -> notifyItemChanged(0)
        }
    }

    override fun getItemCount() = if (message == null) 0 else 1
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ViewHolder(
        LayoutInflater.from(parent.context).inflate(R.layout.item_home_genre_status, parent, false) as TextView,
    )
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        message?.let(holder.text::setText)
        holder.text.isClickable = canRetry
        holder.text.isFocusable = canRetry
    }
    inner class ViewHolder(val text: TextView) : RecyclerView.ViewHolder(text) {
        init { text.onClickListener { if (canRetry) onRetry() } }
    }
}
