package com.gamefileinspector

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.gamefileinspector.adapters.GameListAdapter
import com.gamefileinspector.databinding.ActivityMainBinding
import com.gamefileinspector.models.GameInfo
import com.gamefileinspector.utils.GameScanner
import com.gamefileinspector.utils.PermissionHelper

class MainActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityMainBinding
    private lateinit var gameListAdapter: GameListAdapter
    private val gameScanner = GameScanner()
    
    private val storagePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            initializeApp()
        } else {
            showPermissionDeniedDialog()
        }
    }
    
    private val manageStorageLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (Environment.isExternalStorageManager()) {
                initializeApp()
            } else {
                showPermissionDeniedDialog()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupUI()
        checkPermissions()
    }
    
    private fun setupUI() {
        setSupportActionBar(binding.toolbar)
        
        gameListAdapter = GameListAdapter { gameInfo ->
            openGameScanner(gameInfo)
        }
        
        binding.recyclerViewGames.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = gameListAdapter
        }
        
        binding.fabScanFiles.setOnClickListener {
            startActivity(Intent(this, GameScannerActivity::class.java))
        }
        
        binding.buttonDemoMode.setOnClickListener {
            startDemoMode()
        }
        
        binding.swipeRefreshLayout.setOnRefreshListener {
            scanForGames()
        }
    }
    
    private fun checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                showStoragePermissionDialog()
            } else {
                initializeApp()
            }
        } else {
            val permissions = arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
            
            val needsPermission = permissions.any {
                ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
            }
            
            if (needsPermission) {
                storagePermissionLauncher.launch(permissions)
            } else {
                initializeApp()
            }
        }
    }
    
    private fun showStoragePermissionDialog() {
        AlertDialog.Builder(this)
            .setTitle("Storage Permission Required")
            .setMessage("This app needs access to storage to analyze game files. Please grant 'All files access' permission.")
            .setPositiveButton("Grant Permission") { _, _ ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                    intent.data = Uri.parse("package:$packageName")
                    manageStorageLauncher.launch(intent)
                }
            }
            .setNegativeButton("Cancel") { _, _ ->
                showPermissionDeniedDialog()
            }
            .setCancelable(false)
            .show()
    }
    
    private fun showPermissionDeniedDialog() {
        AlertDialog.Builder(this)
            .setTitle("Permission Required")
            .setMessage("Storage permission is required to analyze game files. The app cannot function without this permission.")
            .setPositiveButton("Retry") { _, _ ->
                checkPermissions()
            }
            .setNegativeButton("Exit") { _, _ ->
                finish()
            }
            .setCancelable(false)
            .show()
    }
    
    private fun initializeApp() {
        binding.swipeRefreshLayout.isRefreshing = true
        scanForGames()
    }
    
    private fun scanForGames() {
        gameScanner.scanInstalledGames(this) { games ->
            runOnUiThread {
                binding.swipeRefreshLayout.isRefreshing = false
                gameListAdapter.updateGames(games)
                
                if (games.isEmpty()) {
                    Toast.makeText(this, "No games found with accessible data", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
    
    private fun openGameScanner(gameInfo: GameInfo) {
        val intent = Intent(this, GameScannerActivity::class.java).apply {
            putExtra("game_info", gameInfo)
        }
        startActivity(intent)
    }
    
    private fun startDemoMode() {
        startActivity(Intent(this, DemoModeActivity::class.java))
    }
}