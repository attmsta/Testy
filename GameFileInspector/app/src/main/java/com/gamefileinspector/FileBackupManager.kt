package com.gamefileinspector

import com.gamefileinspector.models.GameFile
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

object FileBackupManager {
    
    private val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
    
    fun createBackup(gameFile: GameFile): Boolean {
        return try {
            val originalFile = File(gameFile.path)
            if (!originalFile.exists() || !originalFile.canRead()) {
                return false
            }
            
            val backupDir = File(originalFile.parent, ".backups")
            if (!backupDir.exists()) {
                backupDir.mkdirs()
            }
            
            val timestamp = dateFormat.format(Date())
            val backupFileName = "${originalFile.nameWithoutExtension}_backup_$timestamp.${originalFile.extension}"
            val backupFile = File(backupDir, backupFileName)
            
            originalFile.copyTo(backupFile, overwrite = true)
            
            // Keep only the last 5 backups
            cleanupOldBackups(backupDir, originalFile.nameWithoutExtension)
            
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    
    fun restoreBackup(gameFile: GameFile): Boolean {
        return try {
            val originalFile = File(gameFile.path)
            val backupDir = File(originalFile.parent, ".backups")
            
            if (!backupDir.exists()) {
                return false
            }
            
            // Find the most recent backup
            val backupFiles = backupDir.listFiles { file ->
                file.name.startsWith("${originalFile.nameWithoutExtension}_backup_")
            }?.sortedByDescending { it.lastModified() }
            
            val latestBackup = backupFiles?.firstOrNull() ?: return false
            
            latestBackup.copyTo(originalFile, overwrite = true)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    
    private fun cleanupOldBackups(backupDir: File, fileNameWithoutExtension: String) {
        try {
            val backupFiles = backupDir.listFiles { file ->
                file.name.startsWith("${fileNameWithoutExtension}_backup_")
            }?.sortedByDescending { it.lastModified() }
            
            if (backupFiles != null && backupFiles.size > 5) {
                for (i in 5 until backupFiles.size) {
                    backupFiles[i].delete()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    fun getBackupList(gameFile: GameFile): List<File> {
        return try {
            val originalFile = File(gameFile.path)
            val backupDir = File(originalFile.parent, ".backups")
            
            if (!backupDir.exists()) {
                return emptyList()
            }
            
            backupDir.listFiles { file ->
                file.name.startsWith("${originalFile.nameWithoutExtension}_backup_")
            }?.sortedByDescending { it.lastModified() }?.toList() ?: emptyList()
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
}