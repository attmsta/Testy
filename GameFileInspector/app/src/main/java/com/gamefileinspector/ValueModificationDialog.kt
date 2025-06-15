package com.gamefileinspector

import android.app.Dialog
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.gamefileinspector.databinding.DialogValueModificationBinding
import com.gamefileinspector.models.GameFile
import com.gamefileinspector.models.PossibleValue
import com.gamefileinspector.utils.FileModifier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ValueModificationDialog : DialogFragment() {
    
    private lateinit var binding: DialogValueModificationBinding
    private lateinit var possibleValue: PossibleValue
    private lateinit var gameFile: GameFile
    private var onValueModifiedListener: ((Boolean) -> Unit)? = null
    
    companion object {
        fun newInstance(possibleValue: PossibleValue, gameFile: GameFile): ValueModificationDialog {
            val dialog = ValueModificationDialog()
            val args = Bundle().apply {
                putParcelable("possible_value", possibleValue)
                putParcelable("game_file", gameFile)
            }
            dialog.arguments = args
            return dialog
        }
    }
    
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        binding = DialogValueModificationBinding.inflate(layoutInflater)
        
        possibleValue = arguments?.getParcelable("possible_value") ?: throw IllegalArgumentException("Missing possible value")
        gameFile = arguments?.getParcelable("game_file") ?: throw IllegalArgumentException("Missing game file")
        
        setupUI()
        
        return AlertDialog.Builder(requireContext())
            .setTitle("Modify Value")
            .setView(binding.root)
            .setPositiveButton("Save") { _, _ ->
                modifyValue()
            }
            .setNegativeButton("Cancel", null)
            .create()
    }
    
    private fun setupUI() {
        binding.apply {
            textOriginalValue.text = possibleValue.originalValue
            textDataType.text = possibleValue.dataType.name
            textDescription.text = possibleValue.description ?: "Unknown"
            textOffset.text = "0x${possibleValue.offset.toString(16).uppercase()}"
            
            editNewValue.setText(possibleValue.originalValue)
            editNewValue.selectAll()
        }
    }
    
    private fun modifyValue() {
        val newValue = binding.editNewValue.text.toString().trim()
        
        if (newValue.isEmpty()) {
            Toast.makeText(context, "Please enter a new value", Toast.LENGTH_SHORT).show()
            return
        }
        
        if (newValue == possibleValue.originalValue) {
            Toast.makeText(context, "New value is the same as original", Toast.LENGTH_SHORT).show()
            return
        }
        
        lifecycleScope.launch {
            try {
                val success = withContext(Dispatchers.IO) {
                    FileModifier.modifyValue(gameFile, possibleValue, newValue)
                }
                
                onValueModifiedListener?.invoke(success)
                
                if (success) {
                    Toast.makeText(context, "Value modified successfully!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Failed to modify value", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                onValueModifiedListener?.invoke(false)
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
    
    fun setOnValueModifiedListener(listener: (Boolean) -> Unit) {
        onValueModifiedListener = listener
    }
}