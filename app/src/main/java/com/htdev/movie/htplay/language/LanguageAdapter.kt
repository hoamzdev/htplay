package com.htdev.movie.htplay.language

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.htdev.movie.htplay.databinding.ItemLanguageBinding

class LanguageAdapter(
    val onSelectedLanguage : (String) -> Unit
): ListAdapter<LanguageModel, LanguageAdapter.LanguageViewHolder>(diffUtil) {
    var currentPositionSelected : Int = -1

    fun selectLanguage(code: String) {
        val position = currentList.indexOfFirst { it.code == code }
        if (position == currentPositionSelected) return
        val old = currentPositionSelected
        currentPositionSelected = position
        if (old in currentList.indices) notifyItemChanged(old)
        if (position >= 0) notifyItemChanged(position)
    }
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): LanguageViewHolder {
        val inflate = LayoutInflater.from(parent.context)
        return LanguageViewHolder(ItemLanguageBinding.inflate(inflate,parent,false))
    }

    override fun onBindViewHolder(
        holder: LanguageViewHolder,
        position: Int
    ) {
        val model = getItem(position)
        holder.bind(model, position)
    }

    inner class LanguageViewHolder(val binding: ItemLanguageBinding) :
        RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                val position = bindingAdapterPosition
                if(position == RecyclerView.NO_POSITION || position == currentPositionSelected) return@setOnClickListener
                val code = getItem(position).code
                selectLanguage(code)
                onSelectedLanguage(code)
            }
        }

        fun bind(model: LanguageModel, position: Int) {
            binding.apply {
                ivLogoCountry.setImageResource(model.image)
                tvName.text = model.name
                val isSelected = currentPositionSelected == position
                selectedContainer.visibility = if(isSelected) View.VISIBLE else View.GONE
                ivChecked.visibility = if(isSelected) View.VISIBLE else View.GONE
            }
        }
    }

    companion object {
        val diffUtil = object : DiffUtil.ItemCallback<LanguageModel>() {
            override fun areItemsTheSame(
                oldItem: LanguageModel,
                newItem: LanguageModel
            ) = newItem.code == oldItem.code

            override fun areContentsTheSame(
                oldItem: LanguageModel,
                newItem: LanguageModel
            ) = newItem == oldItem
        }
    }

}
