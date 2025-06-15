package com.gamefileinspector

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.gamefileinspector.adapters.PossibleValueAdapter
import com.gamefileinspector.analyzers.FileAnalyzer
import com.gamefileinspector.databinding.ActivityFileAnalysisBinding
import com.gamefileinspector.models.FileAnalysis
import com.gamefileinspector.models.GameFile
import com.gamefileinspector.models.GameInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FileAnalysisActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityFileAnalysisBinding
    private lateinit var possibleValueAdapter: PossibleValueAdapter
    private lateinit var gameFile: GameFile
    private lateinit var gameInfo: GameInfo
    private lateinit var fileAnalyzer: FileAnalyzer
    private var fileAnalysis: FileAnalysis? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFileAnalysisBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        gameFile = intent.getParcelableExtra("game_file") ?: run {
            Toast.makeText(this, "Error loading file info", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        
        gameInfo = intent.getParcelableExtra("game_info") ?: run {
            Toast.makeText(this, "Error loading game info", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        
        fileAnalyzer = FileAnalyzer()
        setupUI()
        analyzeFile()
    }
    
    private fun setupUI() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = gameFile.name
        
        binding.textFileName.text = gameFile.name
        binding.textFilePath.text = gameFile.path
        binding.textFileSize.text = android.text.format.Formatter.formatFileSize(this, gameFile.size)
        
        possibleValueAdapter = PossibleValueAdapter { possibleValue ->
            // Handle value modification
            showValueModificationDialog(possibleValue)
        }
        
        binding.recyclerViewValues.apply {
            layoutManager = LinearLayoutManager(this@FileAnalysisActivity)
            adapter = possibleValueAdapter
        }
        
        binding.swipeRefreshLayout.setOnRefreshListener {
            analyzeFile()
        }
    }
    
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_file_analysis, menu)
        return true
    }
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_hex_editor -> {
                openHexEditor()
                true
            }
            R.id.action_backup_file -> {
                backupFile()
                true
            }
            R.id.action_restore_file -> {
                restoreFile()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
    
    private fun analyzeFile() {
        binding.swipeRefreshLayout.isRefreshing = true
        
        lifecycleScope.launch {
            try {
                val analysis = withContext(Dispatchers.IO) {
                    fileAnalyzer.analyzeFile(gameFile)
                }
                
                fileAnalysis = analysis
                updateUI(analysis)
            } catch (e: Exception) {
                Toast.makeText(this@FileAnalysisActivity, "Error analyzing file: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                binding.swipeRefreshLayout.isRefreshing = false
            }
        }
    }
    
    private fun updateUI(analysis: FileAnalysis) {
        binding.textFileEncoding.text = analysis.encoding ?: "Binary"
        binding.textFileStructure.text = when (analysis.structure) {
            is com.gamefileinspector.models.FileStructure.Json -> "JSON (${analysis.structure.keys.size} keys)"
            is com.gamefileinspector.models.FileStructure.Xml -> "XML (${analysis.structure.elements.size} elements)"
            is com.gamefileinspector.models.FileStructure.KeyValue -> "Key-Value (${analysis.structure.pairs.size} pairs)"
            is com.gamefileinspector.models.FileStructure.PlainText -> "Plain Text"
            is com.gamefileinspector.models.FileStructure.Binary -> "Binary"
            is com.gamefileinspector.models.FileStructure.Database -> "Database (${analysis.structure.tables.size} tables)"
        }
        
        binding.textPossibleValues.text = "${analysis.possibleValues.size} possible game values found"
        
        possibleValueAdapter.updateValues(analysis.possibleValues)
        
        if (analysis.possibleValues.isEmpty()) {
            Toast.makeText(this, "No obvious game values found. Try the hex editor for manual inspection.", Toast.LENGTH_LONG).show()
        }
    }
    
    private fun showValueModificationDialog(possibleValue: com.gamefileinspector.models.PossibleValue) {
        val dialog = ValueModificationDialog.newInstance(possibleValue, gameFile)
        dialog.setOnValueModifiedListener { success ->
            if (success) {
                Toast.makeText(this, "Value modified successfully!", Toast.LENGTH_SHORT).show()
                analyzeFile() // Re-analyze to show updated values
            } else {
                Toast.makeText(this, "Failed to modify value", Toast.LENGTH_SHORT).show()
            }
        }
        dialog.show(supportFragmentManager, "value_modification")
    }
    
    private fun openHexEditor() {
        val intent = Intent(this, HexEditorActivity::class.java).apply {
            putExtra("game_file", gameFile)
            putExtra("game_info", gameInfo)
        }
        startActivity(intent)
    }
    
    private fun backupFile() {
        lifecycleScope.launch {
            try {
                val success = withContext(Dispatchers.IO) {
                    FileBackupManager.createBackup(gameFile)
                }
                
                if (success) {
                    Toast.makeText(this@FileAnalysisActivity, "File backed up successfully", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@FileAnalysisActivity, "Failed to backup file", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@FileAnalysisActivity, "Error backing up file: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
    
    private fun restoreFile() {
        lifecycleScope.launch {
            try {
                val success = withContext(Dispatchers.IO) {
                    FileBackupManager.restoreBackup(gameFile)
                }
                
                if (success) {
                    Toast.makeText(this@FileAnalysisActivity, "File restored successfully", Toast.LENGTH_SHORT).show()
                    analyzeFile() // Re-analyze the restored file
                } else {
                    Toast.makeText(this@FileAnalysisActivity, "No backup found or failed to restore", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@FileAnalysisActivity, "Error restoring file: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
    
    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}