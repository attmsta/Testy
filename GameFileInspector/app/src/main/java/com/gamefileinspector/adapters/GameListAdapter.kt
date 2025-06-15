package com.gamefileinspector.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.gamefileinspector.databinding.ItemGameBinding
import com.gamefileinspector.models.GameInfo

class GameListAdapter(
    private val onGameClick: (GameInfo) -> Unit
) : RecyclerView.Adapter<GameListAdapter.GameViewHolder>() {
    
    private var games = listOf<GameInfo>()
    
    fun updateGames(newGames: List<GameInfo>) {
        val diffCallback = GameDiffCallback(games, newGames)
        val diffResult = DiffUtil.calculateDiff(diffCallback)
        games = newGames
        diffResult.dispatchUpdatesTo(this)
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GameViewHolder {
        val binding = ItemGameBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return GameViewHolder(binding)
    }
    
    override fun onBindViewHolder(holder: GameViewHolder, position: Int) {
        holder.bind(games[position])
    }
    
    override fun getItemCount(): Int = games.size
    
    inner class GameViewHolder(
        private val binding: ItemGameBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        
        fun bind(gameInfo: GameInfo) {
            binding.apply {
                textGameName.text = gameInfo.appName
                textPackageName.text = gameInfo.packageName
                textFileCount.text = "${gameInfo.gameFiles.size} files found"
                
                val accessibleFiles = gameInfo.gameFiles.count { it.isWritable }
                textAccessibleFiles.text = "$accessibleFiles modifiable files"
                
                root.setOnClickListener {
                    onGameClick(gameInfo)
                }
            }
        }
    }
    
    private class GameDiffCallback(
        private val oldList: List<GameInfo>,
        private val newList: List<GameInfo>
    ) : DiffUtil.Callback() {
        
        override fun getOldListSize(): Int = oldList.size
        override fun getNewListSize(): Int = newList.size
        
        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition].packageName == newList[newItemPosition].packageName
        }
        
        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition] == newList[newItemPosition]
        }
    }
}