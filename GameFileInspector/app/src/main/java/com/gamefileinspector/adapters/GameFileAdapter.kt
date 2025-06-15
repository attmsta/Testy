package com.gamefileinspector.adapters

import android.text.format.Formatter
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.gamefileinspector.R
import com.gamefileinspector.databinding.ItemGameFileBinding
import com.gamefileinspector.models.FileType
import com.gamefileinspector.models.GameFile
import java.text.SimpleDateFormat
import java.util.*

class GameFileAdapter(
    private val onFileClick: (GameFile) -> Unit
) : RecyclerView.Adapter<GameFileAdapter.FileViewHolder>() {
    
    private var files = listOf<GameFile>()
    private val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
    
    fun updateFiles(newFiles: List<GameFile>) {
        val diffCallback = FileDiffCallback(files, newFiles)
        val diffResult = DiffUtil.calculateDiff(diffCallback)
        files = newFiles
        diffResult.dispatchUpdatesTo(this)
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FileViewHolder {
        val binding = ItemGameFileBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return FileViewHolder(binding)
    }
    
    override fun onBindViewHolder(holder: FileViewHolder, position: Int) {
        holder.bind(files[position])
    }
    
    override fun getItemCount(): Int = files.size
    
    inner class FileViewHolder(
        private val binding: ItemGameFileBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        
        fun bind(gameFile: GameFile) {
            binding.apply {
                textFileName.text = gameFile.name
                textFilePath.text = gameFile.path
                textFileSize.text = Formatter.formatFileSize(root.context, gameFile.size)
                textLastModified.text = dateFormat.format(Date(gameFile.lastModified))
                
                // Set file type icon and color
                val (iconRes, colorRes) = getFileTypeIconAndColor(gameFile.type)
                imageFileType.setImageResource(iconRes)
                imageFileType.setColorFilter(ContextCompat.getColor(root.context, colorRes))
                
                // Show permissions
                val permissions = mutableListOf<String>()
                if (gameFile.isReadable) permissions.add("R")
                if (gameFile.isWritable) permissions.add("W")
                textPermissions.text = permissions.joinToString("/")
                
                // Set background color based on writability
                if (gameFile.isWritable) {
                    cardView.setCardBackgroundColor(
                        ContextCompat.getColor(root.context, R.color.writable_file_background)
                    )
                } else {
                    cardView.setCardBackgroundColor(
                        ContextCompat.getColor(root.context, R.color.readonly_file_background)
                    )
                }
                
                root.setOnClickListener {
                    onFileClick(gameFile)
                }
            }
        }
        
        private fun getFileTypeIconAndColor(fileType: FileType): Pair<Int, Int> {
            return when (fileType) {
                FileType.SAVE_FILE -> Pair(R.drawable.ic_save, R.color.save_file_color)
                FileType.CONFIG_FILE -> Pair(R.drawable.ic_settings, R.color.config_file_color)
                FileType.DATABASE -> Pair(R.drawable.ic_database, R.color.database_file_color)
                FileType.JSON -> Pair(R.drawable.ic_code, R.color.json_file_color)
                FileType.XML -> Pair(R.drawable.ic_code, R.color.xml_file_color)
                FileType.PREFERENCES -> Pair(R.drawable.ic_tune, R.color.pref_file_color)
                FileType.BINARY -> Pair(R.drawable.ic_binary, R.color.binary_file_color)
                FileType.UNKNOWN -> Pair(R.drawable.ic_file, R.color.unknown_file_color)
            }
        }
    }
    
    private class FileDiffCallback(
        private val oldList: List<GameFile>,
        private val newList: List<GameFile>
    ) : DiffUtil.Callback() {
        
        override fun getOldListSize(): Int = oldList.size
        override fun getNewListSize(): Int = newList.size
        
        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition].path == newList[newItemPosition].path
        }
        
        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition] == newList[newItemPosition]
        }
    }
}