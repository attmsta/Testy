package com.gamefileinspector.utils

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Environment
import com.gamefileinspector.models.FileType
import com.gamefileinspector.models.GameFile
import com.gamefileinspector.models.GameInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class GameScanner {
    
    fun scanInstalledGames(context: Context, callback: (List<GameInfo>) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            // Use enhanced scanner for better detection
            val enhancedScanner = EnhancedGameScanner()
            val enhancedGames = try {
                enhancedScanner.scanForGamesEnhanced(context)
            } catch (e: Exception) {
                emptyList()
            }
            
            // If enhanced scanner found games, use those
            if (enhancedGames.isNotEmpty()) {
                withContext(Dispatchers.Main) {
                    callback(enhancedGames.sortedBy { it.name })
                }
                return@launch
            }
            
            // Fallback to original scanning
            val games = mutableListOf<GameInfo>()
            val packageManager = context.packageManager
            
            try {
                val installedApps = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
                
                for (appInfo in installedApps) {
                    if (isGameApp(appInfo, packageManager)) {
                        val gameInfo = analyzeGameApp(appInfo, context)
                        if (gameInfo.hasAccessibleData) {
                            games.add(gameInfo)
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            
            withContext(Dispatchers.Main) {
                callback(games.sortedBy { it.appName })
            }
        }
    }
    
    private fun isGameApp(appInfo: ApplicationInfo, packageManager: PackageManager): Boolean {
        // Check if app is in Games category
        if (appInfo.category == ApplicationInfo.CATEGORY_GAME) {
            return true
        }
        
        // Check common game package patterns
        val gamePatterns = listOf(
            "game", "puzzle", "arcade", "action", "adventure", "strategy",
            "simulation", "racing", "sports", "casino", "card", "board"
        )
        
        val packageName = appInfo.packageName.lowercase()
        val appName = packageManager.getApplicationLabel(appInfo).toString().lowercase()
        
        return gamePatterns.any { pattern ->
            packageName.contains(pattern) || appName.contains(pattern)
        }
    }
    
    private fun analyzeGameApp(appInfo: ApplicationInfo, context: Context): GameInfo {
        val packageManager = context.packageManager
        val appName = packageManager.getApplicationLabel(appInfo).toString()
        val packageName = appInfo.packageName
        
        // Scan accessible data directories
        val gameFiles = mutableListOf<GameFile>()
        var hasAccessibleData = false
        
        // Check external storage directories (accessible without root)
        val externalDataPath = getExternalDataPath(packageName)
        val obbPath = getObbPath(packageName)
        
        // Scan external data directory
        externalDataPath?.let { path ->
            val files = scanDirectory(File(path))
            gameFiles.addAll(files)
            if (files.isNotEmpty()) hasAccessibleData = true
        }
        
        // Scan OBB directory
        obbPath?.let { path ->
            val files = scanDirectory(File(path))
            gameFiles.addAll(files)
            if (files.isNotEmpty()) hasAccessibleData = true
        }
        
        // Check shared storage for game-specific folders
        val sharedGameFiles = scanSharedStorage(packageName, appName)
        gameFiles.addAll(sharedGameFiles)
        if (sharedGameFiles.isNotEmpty()) hasAccessibleData = true
        
        return GameInfo(
            packageName = packageName,
            appName = appName,
            dataPath = appInfo.dataDir,
            externalDataPath = externalDataPath,
            obbPath = obbPath,
            hasAccessibleData = hasAccessibleData,
            gameFiles = gameFiles
        )
    }
    
    private fun getExternalDataPath(packageName: String): String? {
        val externalStorage = Environment.getExternalStorageDirectory()
        val androidDataPath = File(externalStorage, "Android/data/$packageName")
        return if (androidDataPath.exists() && androidDataPath.canRead()) {
            androidDataPath.absolutePath
        } else null
    }
    
    private fun getObbPath(packageName: String): String? {
        val externalStorage = Environment.getExternalStorageDirectory()
        val obbPath = File(externalStorage, "Android/obb/$packageName")
        return if (obbPath.exists() && obbPath.canRead()) {
            obbPath.absolutePath
        } else null
    }
    
    private fun scanSharedStorage(packageName: String, appName: String): List<GameFile> {
        val gameFiles = mutableListOf<GameFile>()
        val externalStorage = Environment.getExternalStorageDirectory()
        
        // Common game data locations in shared storage
        val commonPaths = listOf(
            appName,
            packageName.substringAfterLast('.'),
            "Games/$appName",
            "games/$appName",
            ".${packageName}",
            ".$appName"
        )
        
        for (path in commonPaths) {
            val gameDir = File(externalStorage, path)
            if (gameDir.exists() && gameDir.canRead()) {
                gameFiles.addAll(scanDirectory(gameDir))
            }
        }
        
        return gameFiles
    }
    
    private fun scanDirectory(directory: File): List<GameFile> {
        val gameFiles = mutableListOf<GameFile>()
        
        try {
            directory.listFiles()?.forEach { file ->
                if (file.isFile && file.canRead()) {
                    val gameFile = GameFile(
                        name = file.name,
                        path = file.absolutePath,
                        size = file.length(),
                        lastModified = file.lastModified(),
                        type = determineFileType(file),
                        isReadable = file.canRead(),
                        isWritable = file.canWrite()
                    )
                    gameFiles.add(gameFile)
                } else if (file.isDirectory && file.canRead()) {
                    // Recursively scan subdirectories (limit depth to avoid performance issues)
                    if (directory.absolutePath.split("/").size < 8) {
                        gameFiles.addAll(scanDirectory(file))
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        return gameFiles
    }
    
    private fun determineFileType(file: File): FileType {
        val fileName = file.name.lowercase()
        val extension = file.extension.lowercase()
        
        return when {
            fileName.contains("save") || fileName.contains("progress") -> FileType.SAVE_FILE
            fileName.contains("config") || fileName.contains("setting") -> FileType.CONFIG_FILE
            extension == "db" || extension == "sqlite" || extension == "sqlite3" -> FileType.DATABASE
            extension == "json" -> FileType.JSON
            extension == "xml" -> FileType.XML
            fileName.contains("pref") || extension == "pref" -> FileType.PREFERENCES
            extension in listOf("dat", "bin", "data") -> FileType.BINARY
            else -> FileType.UNKNOWN
        }
    }
}