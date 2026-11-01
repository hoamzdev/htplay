package com.htdev.movie.htplay.favorite

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.htdev.movie.htplay.databinding.ItemMovieSelectFavoriteBinding
import com.htdev.movie.htplay.util.loadPoster
import com.htdev.movie.htplay.widget.gone
import com.htdev.movie.htplay.widget.visible
import java.util.UUID

data class ChooseFavoriteItem(
    val uuid: UUID = UUID.randomUUID(),
    val nameGenre: String,
    val urlPoster: String
)

class ChooseFavoriteAdapter : ListAdapter<ChooseFavoriteItem, ChooseFavoriteAdapter.ChooseFavoriteViewHolder>(diffUtil) {

    private val selectedIds = mutableSetOf<UUID>()

    fun getSelectedGenres() : List<String>{
        return currentList.filter { it.uuid in selectedIds }.map { it.nameGenre }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ChooseFavoriteViewHolder {
        val inflate = LayoutInflater.from(parent.context)
        val binding: ItemMovieSelectFavoriteBinding =
            ItemMovieSelectFavoriteBinding.inflate(inflate, parent, false)
        return ChooseFavoriteViewHolder(binding)
    }

    override fun onBindViewHolder(
        holder: ChooseFavoriteViewHolder,
        position: Int
    ) {
        holder.bind(getItem(position))
    }

    override fun onBindViewHolder(
        holder: ChooseFavoriteViewHolder,
        position: Int,
        payloads: MutableList<Any>
    ) {
        if (payloads.isNotEmpty() && payloads.all { it == SELECTION_CHANGED }) {
            holder.bindSelection(getItem(position))
        } else {
            holder.bind(getItem(position))
        }
    }

    override fun onCurrentListChanged(
        previousList: MutableList<ChooseFavoriteItem>,
        currentList: MutableList<ChooseFavoriteItem>
    ) {
        super.onCurrentListChanged(previousList, currentList)
        selectedIds.retainAll(currentList.map { it.uuid }.toSet())
    }

    inner class ChooseFavoriteViewHolder(val binding: ItemMovieSelectFavoriteBinding) :
        RecyclerView.ViewHolder(binding.root) {


        init {
            binding.root.setOnClickListener {
                val position = bindingAdapterPosition
                if (position == RecyclerView.NO_POSITION) return@setOnClickListener
                val item = getItem(position)
                if(selectedIds.size < 3){
                    if (!selectedIds.remove(item.uuid)) {
                        selectedIds.add(item.uuid)
                    }
                    notifyItemChanged(position, SELECTION_CHANGED)
                }
            }
        }

        fun bind(item: ChooseFavoriteItem) {
            binding.apply {
                imgPoster.loadPoster(item.urlPoster)
                tvName.text = item.nameGenre
            }
            bindSelection(item)
        }

        fun bindSelection(item: ChooseFavoriteItem) {
            val selected = item.uuid in selectedIds
            binding.root.isSelected = selected
            if (selected) {
                binding.cdSelected.visible()
            } else {
                binding.cdSelected.gone()
            }
        }
    }

    companion object {
        private const val SELECTION_CHANGED = "selection_changed"
        val diffUtil = object : DiffUtil.ItemCallback<ChooseFavoriteItem>() {
            override fun areItemsTheSame(
                oldItem: ChooseFavoriteItem,
                newItem: ChooseFavoriteItem
            ): Boolean {
                return newItem.uuid == oldItem.uuid
            }

            override fun areContentsTheSame(
                oldItem: ChooseFavoriteItem,
                newItem: ChooseFavoriteItem
            ): Boolean {
                return newItem == oldItem
            }
        }
    }
}
