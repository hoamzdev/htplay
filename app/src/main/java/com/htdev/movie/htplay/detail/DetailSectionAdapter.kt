package com.htdev.movie.htplay.detail

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView

/** A single header or recommendations section in the detail scrolling list. */
internal class DetailSectionAdapter(private val section: View) :
    RecyclerView.Adapter<DetailSectionAdapter.SectionViewHolder>() {

    override fun getItemCount() = 1

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = SectionViewHolder(section)

    override fun onBindViewHolder(holder: SectionViewHolder, position: Int) = Unit

    class SectionViewHolder(view: View) : RecyclerView.ViewHolder(view)
}
