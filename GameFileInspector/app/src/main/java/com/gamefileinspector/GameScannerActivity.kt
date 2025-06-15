package com.gamefileinspector

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.gamefileinspector.adapters.GameFileAdapter
import com.gamefileinspector.databinding.ActivityGameScannerBinding
import com.gamefileinspector.models.GameInfo

class GameScannerActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityGameScannerBinding
    private lateinit var gameFileAdapter: GameFileAdapter
    private lateinit var gameInfo: GameInfo
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGameScannerBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        gameInfo = intent.getParcelableExtra("game_info") ?: run {
            Toast.makeText(this, "Error loading game info", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        
        setupUI()
        loadGameFiles()
    }
    
    private fun setupUI() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = gameInfo.appName
        
        binding.textGamePackage.text = gameInfo.packageName
        binding.textFileCount.text = "${gameInfo.gameFiles.size} files found"
        
        gameFileAdapter = GameFileAdapter { gameFile ->
            val intent = Intent(this, FileAnalysisActivity::class.java).apply {
                putExtra("game_file", gameFile)
                putExtra("game_info", gameInfo)
            }
            startActivity(intent)
        }
        
        binding.recyclerViewFiles.apply {
            layoutManager = LinearLayoutManager(this@GameScannerActivity)
            adapter = gameFileAdapter
        }
    }
    
    private fun loadGameFiles() {
        gameFileAdapter.updateFiles(gameInfo.gameFiles)
        
        if (gameInfo.gameFiles.isEmpty()) {
            Toast.makeText(this, "No accessible files found for this game", Toast.LENGTH_LONG).show()
        }
    }
    
    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}