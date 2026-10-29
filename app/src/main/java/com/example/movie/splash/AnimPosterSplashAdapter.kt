package com.example.movie.splash

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.movie.databinding.ItemPosterBinding

class AnimPosterSplashAdapter : RecyclerView.Adapter<AnimPosterSplashAdapter.AnimPosterSplashViewHolder>() {

    private var posters: List<Int> = emptyList()

    // Virtual positions repeat the same small list without allocating more posters.
    override fun getItemCount(): Int = if (posters.isEmpty()) 0 else Int.MAX_VALUE

    val loopStartPosition: Int
        get() = if (posters.isEmpty()) 0 else {
            val middle = Int.MAX_VALUE / 2
            middle - middle % posters.size
        }

    @android.annotation.SuppressLint("NotifyDataSetChanged")
    fun submitList(items: List<Int>) {
        posters = items.toList()
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): AnimPosterSplashViewHolder {
        val binding = ItemPosterBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return AnimPosterSplashViewHolder(binding)
    }

    override fun onBindViewHolder(
        holder: AnimPosterSplashViewHolder,
        position: Int
    ) {
        holder.bind(posters[position % posters.size])
    }

    class AnimPosterSplashViewHolder(val binding: ItemPosterBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(poster: Int) {
            binding.imgPoster.setImageResource(poster)
        }
    }

}
