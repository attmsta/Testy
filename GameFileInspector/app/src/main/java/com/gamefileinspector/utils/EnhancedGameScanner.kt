package com.gamefileinspector.utils

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Environment
import com.gamefileinspector.models.GameFile
import com.gamefileinspector.models.GameInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.regex.Pattern

/**
 * Enhanced game scanner with deep file discovery and analysis
 */
class EnhancedGameScanner {
    
    // Game-related package name patterns
    private val gamePackagePatterns = listOf(
        Pattern.compile(".*\\.game.*", Pattern.CASE_INSENSITIVE),
        Pattern.compile(".*\\.games.*", Pattern.CASE_INSENSITIVE),
        Pattern.compile(".*\\.gaming.*", Pattern.CASE_INSENSITIVE),
        Pattern.compile(".*\\.rpg.*", Pattern.CASE_INSENSITIVE),
        Pattern.compile(".*\\.puzzle.*", Pattern.CASE_INSENSITIVE),
        Pattern.compile(".*\\.strategy.*", Pattern.CASE_INSENSITIVE),
        Pattern.compile(".*\\.action.*", Pattern.CASE_INSENSITIVE),
        Pattern.compile(".*\\.adventure.*", Pattern.CASE_INSENSITIVE),
        Pattern.compile(".*\\.simulation.*", Pattern.CASE_INSENSITIVE),
        Pattern.compile(".*\\.arcade.*", Pattern.CASE_INSENSITIVE),
        Pattern.compile(".*\\.casual.*", Pattern.CASE_INSENSITIVE),
        Pattern.compile(".*\\.racing.*", Pattern.CASE_INSENSITIVE),
        Pattern.compile(".*\\.sports.*", Pattern.CASE_INSENSITIVE),
        Pattern.compile(".*\\.card.*", Pattern.CASE_INSENSITIVE),
        Pattern.compile(".*\\.board.*", Pattern.CASE_INSENSITIVE)
    )
    
    // Known game publishers and developers
    private val gamePublishers = setOf(
        "com.king", "com.supercell", "com.rovio", "com.gameloft", "com.ea",
        "com.activision", "com.ubisoft", "com.square_enix", "com.bandainamco",
        "com.sega", "com.nintendo", "com.sony", "com.microsoft", "com.blizzard",
        "com.epicgames", "com.valve", "com.rockstar", "com.bethesda", "com.mojang",
        "com.mihoyo", "com.netease", "com.tencent", "com.nexon", "com.ncsoft",
        "com.playdead", "com.innersloth", "com.halfbrick", "com.outfit7",
        "com.miniclip", "com.zynga", "com.playrix", "com.moonactive",
        "com.scopely", "com.machinezoneinc", "com.socialpoint", "com.bigfishgames"
    )
    
    // Game-related file extensions and patterns
    private val gameFileExtensions = setOf(
        "sav", "save", "dat", "data", "bin", "db", "sqlite", "sqlite3",
        "json", "xml", "plist", "cfg", "config", "ini", "properties",
        "prefs", "settings", "profile", "user", "player", "character",
        "progress", "stats", "achievements", "inventory", "items"
    )
    
    // Suspicious file name patterns that might contain game data
    private val gameFileNamePatterns = listOf(
        Pattern.compile(".*save.*", Pattern.CASE_INSENSITIVE),
        Pattern.compile(".*player.*", Pattern.CASE_INSENSITIVE),
        Pattern.compile(".*profile.*", Pattern.CASE_INSENSITIVE),
        Pattern.compile(".*progress.*", Pattern.CASE_INSENSITIVE),
        Pattern.compile(".*game.*", Pattern.CASE_INSENSITIVE),
        Pattern.compile(".*user.*", Pattern.CASE_INSENSITIVE),
        Pattern.compile(".*data.*", Pattern.CASE_INSENSITIVE),
        Pattern.compile(".*config.*", Pattern.CASE_INSENSITIVE),
        Pattern.compile(".*settings.*", Pattern.CASE_INSENSITIVE),
        Pattern.compile(".*prefs.*", Pattern.CASE_INSENSITIVE),
        Pattern.compile(".*stats.*", Pattern.CASE_INSENSITIVE),
        Pattern.compile(".*inventory.*", Pattern.CASE_INSENSITIVE),
        Pattern.compile(".*character.*", Pattern.CASE_INSENSITIVE),
        Pattern.compile(".*achievement.*", Pattern.CASE_INSENSITIVE),
        Pattern.compile(".*unlock.*", Pattern.CASE_INSENSITIVE),
        Pattern.compile(".*level.*", Pattern.CASE_INSENSITIVE),
        Pattern.compile(".*score.*", Pattern.CASE_INSENSITIVE),
        Pattern.compile(".*currency.*", Pattern.CASE_INSENSITIVE),
        Pattern.compile(".*gold.*", Pattern.CASE_INSENSITIVE),
        Pattern.compile(".*coin.*", Pattern.CASE_INSENSITIVE),
        Pattern.compile(".*gem.*", Pattern.CASE_INSENSITIVE),
        Pattern.compile(".*diamond.*", Pattern.CASE_INSENSITIVE)
    )
    
    // Common game data directory names
    private val gameDataDirectories = setOf(
        "saves", "save", "data", "gamedata", "userdata", "profiles",
        "progress", "stats", "config", "settings", "cache", "temp",
        "files", "documents", "shared_prefs", "databases", "preferences"
    )
    
    /**
     * Scan for games with enhanced detection
     */
    suspend fun scanForGamesEnhanced(context: Context): List<GameInfo> = withContext(Dispatchers.IO) {
        val games = mutableListOf<GameInfo>()
        val packageManager = context.packageManager
        
        try {
            val installedApps = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
            
            installedApps.forEach { appInfo ->
                if (isLikelyGame(appInfo, packageManager)) {
                    val gameInfo = createGameInfo(appInfo, packageManager, context)
                    if (gameInfo.accessibleFiles.isNotEmpty()) {
                        games.add(gameInfo)
                    }
                }
            }
            
        } catch (e: Exception) {
            // Handle permission errors gracefully
        }
        
        return@withContext games.sortedByDescending { it.accessibleFiles.size }
    }
    
    /**
     * Enhanced game detection logic
     */
    private fun isLikelyGame(appInfo: ApplicationInfo, packageManager: PackageManager): Boolean {
        val packageName = appInfo.packageName
        
        // Skip system apps unless they're games
        if (appInfo.flags and ApplicationInfo.FLAG_SYSTEM != 0) {
            return isKnownSystemGame(packageName)
        }
        
        // Check package name patterns
        if (gamePackagePatterns.any { it.matcher(packageName).matches() }) {
            return true
        }
        
        // Check known publishers
        if (gamePublishers.any { packageName.startsWith(it) }) {
            return true
        }
        
        // Check app category (requires API 26+)
        try {
            val appCategory = appInfo.category
            if (appCategory == ApplicationInfo.CATEGORY_GAME) {
                return true
            }
        } catch (e: Exception) {
            // Category not available on older Android versions
        }
        
        // Check app name for game-related keywords
        try {
            val appName = packageManager.getApplicationLabel(appInfo).toString().lowercase()
            val gameKeywords = setOf(
                "game", "play", "puzzle", "adventure", "action", "strategy",
                "rpg", "simulation", "racing", "sports", "arcade", "casual",
                "card", "board", "word", "trivia", "match", "saga", "quest",
                "hero", "legend", "kingdom", "empire", "war", "battle",
                "craft", "build", "farm", "city", "world", "fantasy"
            )
            
            if (gameKeywords.any { appName.contains(it) }) {
                return true
            }
        } catch (e: Exception) {
            // Unable to get app name
        }
        
        return false
    }
    
    /**
     * Check if system app is a known game
     */
    private fun isKnownSystemGame(packageName: String): Boolean {
        val knownSystemGames = setOf(
            "com.android.chess",
            "com.google.android.apps.games",
            "com.google.android.play.games"
        )
        return knownSystemGames.contains(packageName)
    }
    
    /**
     * Create comprehensive game info with file discovery
     */
    private fun createGameInfo(
        appInfo: ApplicationInfo,
        packageManager: PackageManager,
        context: Context
    ): GameInfo {
        val packageName = appInfo.packageName
        val appName = try {
            packageManager.getApplicationLabel(appInfo).toString()
        } catch (e: Exception) {
            packageName
        }
        
        val accessibleFiles = findAccessibleGameFiles(packageName, context)
        
        return GameInfo(
            name = appName,
            packageName = packageName,
            dataPath = appInfo.dataDir,
            accessibleFiles = accessibleFiles,
            hasWriteAccess = checkWriteAccess(accessibleFiles),
            gameScore = calculateGameScore(packageName, appName, accessibleFiles)
        )
    }
    
    /**
     * Find all accessible game files with deep scanning
     */
    private fun findAccessibleGameFiles(packageName: String, context: Context): List<GameFile> {
        val files = mutableListOf<GameFile>()
        
        // Scan multiple potential locations
        val searchPaths = listOf(
            // External storage paths
            File(Environment.getExternalStorageDirectory(), "Android/data/$packageName"),
            File(Environment.getExternalStorageDirectory(), "Android/obb/$packageName"),
            File(Environment.getExternalStorageDirectory(), packageName),
            File(Environment.getExternalStorageDirectory(), "Games/$packageName"),
            File(Environment.getExternalStorageDirectory(), "games/$packageName"),
            File(Environment.getExternalStorageDirectory(), "data/$packageName"),
            
            // Common game directories
            File(Environment.getExternalStorageDirectory(), "Games"),
            File(Environment.getExternalStorageDirectory(), "games"),
            File(Environment.getExternalStorageDirectory(), "GameData"),
            File(Environment.getExternalStorageDirectory(), "gamedata"),
            
            // Documents and Downloads
            File(Environment.getExternalStorageDirectory(), "Documents/$packageName"),
            File(Environment.getExternalStorageDirectory(), "Download/$packageName"),
            
            // App-specific external directories
            *context.getExternalFilesDirs(null).filterNotNull().toTypedArray(),
            *context.externalCacheDirs.filterNotNull().toTypedArray()
        )
        
        searchPaths.forEach { path ->
            if (path.exists() && path.canRead()) {
                files.addAll(scanDirectoryDeep(path, packageName, maxDepth = 5))
            }
        }
        
        // Remove duplicates and sort by relevance
        return files.distinctBy { it.path }
            .sortedByDescending { calculateFileRelevance(it) }
    }
    
    /**
     * Deep directory scanning with relevance filtering
     */
    private fun scanDirectoryDeep(
        directory: File,
        packageName: String,
        currentDepth: Int = 0,
        maxDepth: Int = 5
    ): List<GameFile> {
        val files = mutableListOf<GameFile>()
        
        if (currentDepth > maxDepth || !directory.exists() || !directory.canRead()) {
            return files
        }
        
        try {
            directory.listFiles()?.forEach { file ->
                when {
                    file.isFile -> {
                        if (isRelevantGameFile(file)) {
                            files.add(createGameFile(file, packageName))
                        }
                    }
                    file.isDirectory -> {
                        // Recursively scan subdirectories
                        if (isRelevantGameDirectory(file)) {
                            files.addAll(scanDirectoryDeep(file, packageName, currentDepth + 1, maxDepth))
                        }
                    }
                }
            }
        } catch (e: SecurityException) {
            // Permission denied, skip this directory
        }
        
        return files
    }
    
    /**
     * Check if file is relevant for game analysis
     */
    private fun isRelevantGameFile(file: File): Boolean {
        val fileName = file.name.lowercase()
        val extension = file.extension.lowercase()
        
        // Check file extension
        if (extension in gameFileExtensions) {
            return true
        }
        
        // Check file name patterns
        if (gameFileNamePatterns.any { it.matcher(fileName).matches() }) {
            return true
        }
        
        // Check file size (skip very large files and very small files)
        val fileSize = file.length()
        if (fileSize < 10 || fileSize > 100 * 1024 * 1024) { // 10 bytes to 100MB
            return false
        }
        
        // Check if file contains potential game data
        return hasGameDataSignatures(file)
    }
    
    /**
     * Check if directory is relevant for game data
     */
    private fun isRelevantGameDirectory(directory: File): Boolean {
        val dirName = directory.name.lowercase()
        
        // Check against known game data directory names
        if (dirName in gameDataDirectories) {
            return true
        }
        
        // Check directory name patterns
        if (gameFileNamePatterns.any { it.matcher(dirName).matches() }) {
            return true
        }
        
        // Skip system directories
        val systemDirs = setOf("cache", "tmp", "temp", "log", "logs", "lib", "libs")
        if (dirName in systemDirs) {
            return false
        }
        
        return true
    }
    
    /**
     * Check if file has game data signatures
     */
    private fun hasGameDataSignatures(file: File): Boolean {
        try {
            val header = file.readBytes().take(1024)
            val headerString = String(header.toByteArray(), Charsets.UTF_8)
            
            // Check for JSON/XML structures
            if (headerString.contains("{") || headerString.contains("<")) {
                return true
            }
            
            // Check for key-value patterns
            if (headerString.contains("=") && headerString.contains("\n")) {
                return true
            }
            
            // Check for SQLite signature
            if (header.take(6).map { it.toInt().toChar() }.joinToString("") == "SQLite") {
                return true
            }
            
            // Check for common game data keywords
            val gameKeywords = setOf(
                "player", "level", "score", "gold", "coin", "gem", "diamond",
                "experience", "health", "mana", "inventory", "item", "weapon",
                "armor", "achievement", "unlock", "progress", "save", "game"
            )
            
            val lowerContent = headerString.lowercase()
            if (gameKeywords.any { lowerContent.contains(it) }) {
                return true
            }
            
            // Check for numeric patterns that might be game values
            val numberPattern = Pattern.compile("\\d{3,}")
            if (numberPattern.matcher(headerString).find()) {
                return true
            }
            
        } catch (e: Exception) {
            // If we can't read the file, assume it might be relevant
            return true
        }
        
        return false
    }
    
    /**
     * Create GameFile object with metadata
     */
    private fun createGameFile(file: File, packageName: String): GameFile {
        return GameFile(
            path = file.absolutePath,
            name = file.name,
            size = file.length(),
            lastModified = file.lastModified(),
            gamePackage = packageName,
            isReadable = file.canRead(),
            isWritable = file.canWrite(),
            extension = file.extension.lowercase(),
            relevanceScore = calculateFileRelevance(file)
        )
    }
    
    /**
     * Calculate file relevance score for prioritization
     */
    private fun calculateFileRelevance(file: File): Double {
        var score = 0.0
        val fileName = file.name.lowercase()
        val extension = file.extension.lowercase()
        
        // Extension scoring
        when (extension) {
            "json" -> score += 0.9
            "xml" -> score += 0.8
            "db", "sqlite", "sqlite3" -> score += 0.7
            "sav", "save" -> score += 0.9
            "dat", "data" -> score += 0.6
            "cfg", "config", "ini", "properties" -> score += 0.7
            "prefs", "settings" -> score += 0.6
            else -> score += 0.3
        }
        
        // File name scoring
        val highValueKeywords = setOf("save", "player", "profile", "progress", "game")
        val mediumValueKeywords = setOf("data", "config", "settings", "stats", "inventory")
        val lowValueKeywords = setOf("user", "prefs", "character", "achievement")
        
        highValueKeywords.forEach { keyword ->
            if (fileName.contains(keyword)) score += 0.3
        }
        mediumValueKeywords.forEach { keyword ->
            if (fileName.contains(keyword)) score += 0.2
        }
        lowValueKeywords.forEach { keyword ->
            if (fileName.contains(keyword)) score += 0.1
        }
        
        // File size scoring (prefer medium-sized files)
        val fileSize = file.length()
        when {
            fileSize in 1024..1024*1024 -> score += 0.2 // 1KB to 1MB
            fileSize in 1024*1024..10*1024*1024 -> score += 0.1 // 1MB to 10MB
            fileSize < 100 -> score -= 0.3 // Very small files
            fileSize > 50*1024*1024 -> score -= 0.2 // Very large files
        }
        
        // Accessibility scoring
        if (file.canWrite()) score += 0.2
        if (file.canRead()) score += 0.1
        
        return score.coerceIn(0.0, 1.0)
    }
    
    /**
     * Calculate file relevance score for GameFile object
     */
    private fun calculateFileRelevance(gameFile: GameFile): Double {
        return calculateFileRelevance(File(gameFile.path))
    }
    
    /**
     * Check write access for files
     */
    private fun checkWriteAccess(files: List<GameFile>): Boolean {
        return files.any { it.isWritable }
    }
    
    /**
     * Calculate overall game score for prioritization
     */
    private fun calculateGameScore(packageName: String, appName: String, files: List<GameFile>): Double {
        var score = 0.0
        
        // Package name scoring
        if (gamePackagePatterns.any { it.matcher(packageName).matches() }) {
            score += 0.3
        }
        
        if (gamePublishers.any { packageName.startsWith(it) }) {
            score += 0.4
        }
        
        // App name scoring
        val gameKeywords = setOf("game", "play", "puzzle", "adventure", "rpg")
        val appNameLower = appName.lowercase()
        gameKeywords.forEach { keyword ->
            if (appNameLower.contains(keyword)) score += 0.1
        }
        
        // File scoring
        score += files.size * 0.05 // More files = higher score
        score += files.sumOf { it.relevanceScore } / files.size.coerceAtLeast(1) * 0.3
        
        // Write access bonus
        if (files.any { it.isWritable }) {
            score += 0.2
        }
        
        return score.coerceIn(0.0, 1.0)
    }
    
    /**
     * Find hidden or obfuscated game files
     */
    fun findHiddenGameFiles(packageName: String, context: Context): List<GameFile> {
        val hiddenFiles = mutableListOf<GameFile>()
        
        // Look for files with unusual extensions but game-like content
        val searchPaths = listOf(
            File(Environment.getExternalStorageDirectory(), "Android/data/$packageName"),
            File(Environment.getExternalStorageDirectory(), packageName)
        )
        
        searchPaths.forEach { path ->
            if (path.exists() && path.canRead()) {
                hiddenFiles.addAll(findObfuscatedFiles(path, packageName))
            }
        }
        
        return hiddenFiles
    }
    
    /**
     * Find files that might be obfuscated or have unusual names
     */
    private fun findObfuscatedFiles(directory: File, packageName: String): List<GameFile> {
        val files = mutableListOf<GameFile>()
        
        try {
            directory.walkTopDown().forEach { file ->
                if (file.isFile && isLikelyObfuscatedGameFile(file)) {
                    files.add(createGameFile(file, packageName))
                }
            }
        } catch (e: Exception) {
            // Permission denied or other error
        }
        
        return files
    }
    
    /**
     * Check if file might be an obfuscated game file
     */
    private fun isLikelyObfuscatedGameFile(file: File): Boolean {
        val fileName = file.name
        
        // Check for files with no extension but reasonable size
        if (!fileName.contains(".") && file.length() in 1024..10*1024*1024) {
            return hasGameDataSignatures(file)
        }
        
        // Check for files with unusual extensions
        val unusualExtensions = setOf("tmp", "bak", "old", "0", "1", "2", "cache")
        if (file.extension.lowercase() in unusualExtensions) {
            return hasGameDataSignatures(file)
        }
        
        // Check for files with random-looking names
        if (fileName.matches(Regex("[a-f0-9]{8,}")) || fileName.matches(Regex("[A-Z0-9]{8,}"))) {
            return hasGameDataSignatures(file)
        }
        
        return false
    }
}