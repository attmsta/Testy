package com.gamefileinspector.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.gamefileinspector.R
import com.gamefileinspector.databinding.ItemPossibleValueBinding
import com.gamefileinspector.models.DataType
import com.gamefileinspector.models.PossibleValue

class PossibleValueAdapter(
    private val onValueClick: (PossibleValue) -> Unit
) : RecyclerView.Adapter<PossibleValueAdapter.ValueViewHolder>() {
    
    private var values = listOf<PossibleValue>()
    
    fun updateValues(newValues: List<PossibleValue>) {
        val diffCallback = ValueDiffCallback(values, newValues)
        val diffResult = DiffUtil.calculateDiff(diffCallback)
        values = newValues
        diffResult.dispatchUpdatesTo(this)
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ValueViewHolder {
        val binding = ItemPossibleValueBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ValueViewHolder(binding)
    }
    
    override fun onBindViewHolder(holder: ValueViewHolder, position: Int) {
        holder.bind(values[position])
    }
    
    override fun getItemCount(): Int = values.size
    
    fun getCurrentValues(): List<PossibleValue> {
        return values.toList()
    }
    
    inner class ValueViewHolder(
        private val binding: ItemPossibleValueBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        
        fun bind(possibleValue: PossibleValue) {
            binding.apply {
                textOriginalValue.text = possibleValue.originalValue
                textDataType.text = possibleValue.dataType.name
                textDescription.text = possibleValue.description ?: "Unknown"
                textOffset.text = "0x${possibleValue.offset.toString(16).uppercase()}"
                textConfidence.text = "${(possibleValue.confidence * 100).toInt()}%"
                
                // Set data type color
                val dataTypeColor = getDataTypeColor(possibleValue.dataType)
                textDataType.setTextColor(ContextCompat.getColor(root.context, dataTypeColor))
                
                // Set confidence color
                val confidenceColor = getConfidenceColor(possibleValue.confidence)
                textConfidence.setTextColor(ContextCompat.getColor(root.context, confidenceColor))
                
                root.setOnClickListener {
                    onValueClick(possibleValue)
                }
            }
        }
        
        private fun getDataTypeColor(dataType: DataType): Int {
            return when (dataType) {
                DataType.CURRENCY -> R.color.currency_color
                DataType.SCORE -> R.color.score_color
                DataType.LEVEL -> R.color.level_color
                DataType.EXPERIENCE -> R.color.experience_color
                DataType.INTEGER -> R.color.integer_color
                DataType.FLOAT -> R.color.float_color
                DataType.STRING -> R.color.string_color
                DataType.BOOLEAN -> R.color.boolean_color
                DataType.UNKNOWN -> R.color.unknown_file_color
            }
        }
        
        private fun getConfidenceColor(confidence: Float): Int {
            return when {
                confidence >= 0.7f -> R.color.high_confidence
                confidence >= 0.4f -> R.color.medium_confidence
                else -> R.color.low_confidence
            }
        }
    }
    
    private class ValueDiffCallback(
        private val oldList: List<PossibleValue>,
        private val newList: List<PossibleValue>
    ) : DiffUtil.Callback() {
        
        override fun getOldListSize(): Int = oldList.size
        override fun getNewListSize(): Int = newList.size
        
        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition].offset == newList[newItemPosition].offset
        }
        
        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition] == newList[newItemPosition]
        }
    }
}