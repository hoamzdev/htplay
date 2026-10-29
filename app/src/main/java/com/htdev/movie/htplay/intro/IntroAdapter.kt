package com.htdev.movie.htplay.intro

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.StringRes
import androidx.recyclerview.widget.RecyclerView
import com.htdev.movie.htplay.R
import com.htdev.movie.htplay.databinding.ItemIntroBinding


data class IntroItem(
    val idImage : Int,
    @StringRes val intro1 : Int,
    @StringRes val intro2 : Int,
)

class IntroAdapter : RecyclerView.Adapter<IntroAdapter.IntroViewHolder>() {

    private var list : List<IntroItem> = listOf()

    fun setListData(list : List<IntroItem>){
        this.list = list
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): IntroViewHolder {
        val inflate = LayoutInflater.from(parent.context)
        val binding = ItemIntroBinding.inflate(inflate, parent, false)
        return IntroViewHolder(binding)
    }

    override fun onBindViewHolder(holder: IntroViewHolder, position: Int) {
        holder.bind(list[position])
    }

    class IntroViewHolder(val binding: ItemIntroBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item : IntroItem){
            binding.apply {
                ivIntro.setImageResource(item.idImage)
            }
        }
    }

    override fun getItemCount(): Int {
        return list.size
    }
}