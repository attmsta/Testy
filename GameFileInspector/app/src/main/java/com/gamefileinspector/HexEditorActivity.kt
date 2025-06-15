package com.gamefileinspector

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.gamefileinspector.databinding.ActivityHexEditorBinding
import com.gamefileinspector.models.GameFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class HexEditorActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityHexEditorBinding
    private lateinit var gameFile: GameFile
    private var fileContent: ByteArray = byteArrayOf()
    private var isModified = false
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHexEditorBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        gameFile = intent.getParcelableExtra("game_file") ?: run {
            finish()
            return
        }
        
        setupToolbar()
        loadFile()
    }
    
    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            title = File(gameFile.path).name
        }
    }
    
    private fun loadFile() {
        lifecycleScope.launch {
            try {
                fileContent = withContext(Dispatchers.IO) {
                    File(gameFile.path).readBytes()
                }
                
                displayHexContent()
            } catch (e: Exception) {
                Toast.makeText(this@HexEditorActivity, "Error loading file: ${e.message}", Toast.LENGTH_LONG).show()
                finish()
            }
        }
    }
    
    private fun displayHexContent() {
        val hexBuilder = StringBuilder()
        val asciiBuilder = StringBuilder()
        val fullBuilder = StringBuilder()
        
        for (i in fileContent.indices) {
            if (i % 16 == 0) {
                if (i > 0) {
                    fullBuilder.append("  ").append(asciiBuilder.toString()).append("\n")
                    asciiBuilder.clear()
                }
                fullBuilder.append(String.format("%08X: ", i))
            }
            
            val byte = fileContent[i].toInt() and 0xFF
            fullBuilder.append(String.format("%02X ", byte))
            
            val char = if (byte in 32..126) byte.toChar() else '.'
            asciiBuilder.append(char)
            
            if (i % 8 == 7) {
                fullBuilder.append(" ")
            }
        }
        
        // Add remaining ASCII characters
        if (asciiBuilder.isNotEmpty()) {
            val remaining = 16 - (fileContent.size % 16)
            if (remaining < 16) {
                repeat(remaining) {
                    fullBuilder.append("   ")
                    if ((fileContent.size + it) % 8 == 7) {
                        fullBuilder.append(" ")
                    }
                }
            }
            fullBuilder.append("  ").append(asciiBuilder.toString())
        }
        
        binding.textHexContent.text = fullBuilder.toString()
    }
    
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_hex_editor, menu)
        return true
    }
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                true
            }
            R.id.action_save -> {
                saveFile()
                true
            }
            R.id.action_search -> {
                showSearchDialog()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
    
    private fun saveFile() {
        if (!isModified) {
            Toast.makeText(this, "No changes to save", Toast.LENGTH_SHORT).show()
            return
        }
        
        lifecycleScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    File(gameFile.path).writeBytes(fileContent)
                }
                
                isModified = false
                Toast.makeText(this@HexEditorActivity, "File saved successfully", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(this@HexEditorActivity, "Error saving file: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
    
    private fun showSearchDialog() {
        // TODO: Implement search functionality
        Toast.makeText(this, "Search functionality coming soon", Toast.LENGTH_SHORT).show()
    }
    
    override fun onBackPressed() {
        if (isModified) {
            // TODO: Show unsaved changes dialog
        }
        super.onBackPressed()
    }
}