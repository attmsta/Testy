package com.gamefileinspector.utils

import com.gamefileinspector.models.GameFile
import org.json.JSONObject
import java.io.File
import java.util.*

/**
 * Utility for comparing files and detecting changes
 */
object FileComparator {
    
    data class FileComparison(
        val originalFile: String,
        val modifiedFile: String,
        val differences: List<FileDifference>,
        val changeCount: Int,
        val comparisonTime: Long = System.currentTimeMillis()
    )
    
    data class FileDifference(
        val type: DifferenceType,
        val location: String,
        val originalValue: String?,
        val newValue: String?,
        val description: String
    )
    
    enum class DifferenceType {
        VALUE_CHANGED,
        VALUE_ADDED,
        VALUE_REMOVED,
        STRUCTURE_CHANGED
    }
    
    /**
     * Compares two files and returns the differences
     */
    fun compareFiles(originalPath: String, modifiedPath: String): FileComparison {
        val originalFile = File(originalPath)
        val modifiedFile = File(modifiedPath)
        
        if (!originalFile.exists() || !modifiedFile.exists()) {
            throw IllegalArgumentException("One or both files do not exist")
        }
        
        val differences = when {
            isJsonFile(originalPath) -> compareJsonFiles(originalFile, modifiedFile)
            isPropertiesFile(originalPath) -> comparePropertiesFiles(originalFile, modifiedFile)
            isBinaryFile(originalPath) -> compareBinaryFiles(originalFile, modifiedFile)
            else -> compareTextFiles(originalFile, modifiedFile)
        }
        
        return FileComparison(
            originalFile = originalPath,
            modifiedFile = modifiedPath,
            differences = differences,
            changeCount = differences.size
        )
    }
    
    /**
     * Compares a file with its backup
     */
    fun compareWithBackup(gameFile: GameFile): FileComparison? {
        val backupPath = FileBackupManager.getLatestBackupPath(gameFile)
        return if (backupPath != null) {
            compareFiles(backupPath, gameFile.path)
        } else {
            null
        }
    }
    
    /**
     * Generates a human-readable summary of changes
     */
    fun generateChangeSummary(comparison: FileComparison): String {
        val summary = StringBuilder()
        summary.append("File Comparison Summary\n")
        summary.append("======================\n")
        summary.append("Original: ${File(comparison.originalFile).name}\n")
        summary.append("Modified: ${File(comparison.modifiedFile).name}\n")
        summary.append("Changes: ${comparison.changeCount}\n\n")
        
        if (comparison.differences.isEmpty()) {
            summary.append("No differences found.\n")
        } else {
            summary.append("Changes detected:\n")
            comparison.differences.forEach { diff ->
                summary.append("• ${diff.description}\n")
                when (diff.type) {
                    DifferenceType.VALUE_CHANGED -> {
                        summary.append("  ${diff.location}: '${diff.originalValue}' → '${diff.newValue}'\n")
                    }
                    DifferenceType.VALUE_ADDED -> {
                        summary.append("  ${diff.location}: Added '${diff.newValue}'\n")
                    }
                    DifferenceType.VALUE_REMOVED -> {
                        summary.append("  ${diff.location}: Removed '${diff.originalValue}'\n")
                    }
                    DifferenceType.STRUCTURE_CHANGED -> {
                        summary.append("  ${diff.location}: Structure modified\n")
                    }
                }
                summary.append("\n")
            }
        }
        
        return summary.toString()
    }
    
    private fun compareJsonFiles(original: File, modified: File): List<FileDifference> {
        val differences = mutableListOf<FileDifference>()
        
        try {
            val originalJson = JSONObject(original.readText())
            val modifiedJson = JSONObject(modified.readText())
            
            compareJsonObjects(originalJson, modifiedJson, "", differences)
        } catch (e: Exception) {
            differences.add(
                FileDifference(
                    type = DifferenceType.STRUCTURE_CHANGED,
                    location = "File structure",
                    originalValue = null,
                    newValue = null,
                    description = "JSON structure comparison failed: ${e.message}"
                )
            )
        }
        
        return differences
    }
    
    private fun compareJsonObjects(
        original: JSONObject,
        modified: JSONObject,
        path: String,
        differences: MutableList<FileDifference>
    ) {
        // Check for removed keys
        original.keys().forEach { key ->
            val currentPath = if (path.isEmpty()) key else "$path.$key"
            if (!modified.has(key)) {
                differences.add(
                    FileDifference(
                        type = DifferenceType.VALUE_REMOVED,
                        location = currentPath,
                        originalValue = original.get(key).toString(),
                        newValue = null,
                        description = "Key '$key' was removed"
                    )
                )
            }
        }
        
        // Check for added or changed keys
        modified.keys().forEach { key ->
            val currentPath = if (path.isEmpty()) key else "$path.$key"
            
            if (!original.has(key)) {
                differences.add(
                    FileDifference(
                        type = DifferenceType.VALUE_ADDED,
                        location = currentPath,
                        originalValue = null,
                        newValue = modified.get(key).toString(),
                        description = "Key '$key' was added"
                    )
                )
            } else {
                val originalValue = original.get(key)
                val modifiedValue = modified.get(key)
                
                if (originalValue is JSONObject && modifiedValue is JSONObject) {
                    compareJsonObjects(originalValue, modifiedValue, currentPath, differences)
                } else if (originalValue.toString() != modifiedValue.toString()) {
                    differences.add(
                        FileDifference(
                            type = DifferenceType.VALUE_CHANGED,
                            location = currentPath,
                            originalValue = originalValue.toString(),
                            newValue = modifiedValue.toString(),
                            description = "Value of '$key' changed"
                        )
                    )
                }
            }
        }
    }
    
    private fun comparePropertiesFiles(original: File, modified: File): List<FileDifference> {
        val differences = mutableListOf<FileDifference>()
        
        try {
            val originalProps = Properties().apply { load(original.inputStream()) }
            val modifiedProps = Properties().apply { load(modified.inputStream()) }
            
            // Check for removed properties
            originalProps.stringPropertyNames().forEach { key ->
                if (!modifiedProps.containsKey(key)) {
                    differences.add(
                        FileDifference(
                            type = DifferenceType.VALUE_REMOVED,
                            location = key,
                            originalValue = originalProps.getProperty(key),
                            newValue = null,
                            description = "Property '$key' was removed"
                        )
                    )
                }
            }
            
            // Check for added or changed properties
            modifiedProps.stringPropertyNames().forEach { key ->
                val originalValue = originalProps.getProperty(key)
                val modifiedValue = modifiedProps.getProperty(key)
                
                when {
                    originalValue == null -> {
                        differences.add(
                            FileDifference(
                                type = DifferenceType.VALUE_ADDED,
                                location = key,
                                originalValue = null,
                                newValue = modifiedValue,
                                description = "Property '$key' was added"
                            )
                        )
                    }
                    originalValue != modifiedValue -> {
                        differences.add(
                            FileDifference(
                                type = DifferenceType.VALUE_CHANGED,
                                location = key,
                                originalValue = originalValue,
                                newValue = modifiedValue,
                                description = "Property '$key' changed"
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            differences.add(
                FileDifference(
                    type = DifferenceType.STRUCTURE_CHANGED,
                    location = "File structure",
                    originalValue = null,
                    newValue = null,
                    description = "Properties file comparison failed: ${e.message}"
                )
            )
        }
        
        return differences
    }
    
    private fun compareBinaryFiles(original: File, modified: File): List<FileDifference> {
        val differences = mutableListOf<FileDifference>()
        
        try {
            val originalBytes = original.readBytes()
            val modifiedBytes = modified.readBytes()
            
            if (originalBytes.size != modifiedBytes.size) {
                differences.add(
                    FileDifference(
                        type = DifferenceType.STRUCTURE_CHANGED,
                        location = "File size",
                        originalValue = "${originalBytes.size} bytes",
                        newValue = "${modifiedBytes.size} bytes",
                        description = "File size changed"
                    )
                )
            }
            
            val minSize = minOf(originalBytes.size, modifiedBytes.size)
            var changeStart = -1
            var changeCount = 0
            
            for (i in 0 until minSize) {
                if (originalBytes[i] != modifiedBytes[i]) {
                    if (changeStart == -1) {
                        changeStart = i
                    }
                    changeCount++
                } else if (changeStart != -1) {
                    // End of a change block
                    differences.add(
                        FileDifference(
                            type = DifferenceType.VALUE_CHANGED,
                            location = "Offset $changeStart-${i-1}",
                            originalValue = "Binary data (${changeCount} bytes)",
                            newValue = "Binary data (${changeCount} bytes)",
                            description = "Binary data changed at offset $changeStart"
                        )
                    )
                    changeStart = -1
                    changeCount = 0
                }
            }
            
            // Handle final change block
            if (changeStart != -1) {
                differences.add(
                    FileDifference(
                        type = DifferenceType.VALUE_CHANGED,
                        location = "Offset $changeStart-${minSize-1}",
                        originalValue = "Binary data (${changeCount} bytes)",
                        newValue = "Binary data (${changeCount} bytes)",
                        description = "Binary data changed at offset $changeStart"
                    )
                )
            }
            
        } catch (e: Exception) {
            differences.add(
                FileDifference(
                    type = DifferenceType.STRUCTURE_CHANGED,
                    location = "File comparison",
                    originalValue = null,
                    newValue = null,
                    description = "Binary file comparison failed: ${e.message}"
                )
            )
        }
        
        return differences
    }
    
    private fun compareTextFiles(original: File, modified: File): List<FileDifference> {
        val differences = mutableListOf<FileDifference>()
        
        try {
            val originalLines = original.readLines()
            val modifiedLines = modified.readLines()
            
            val maxLines = maxOf(originalLines.size, modifiedLines.size)
            
            for (i in 0 until maxLines) {
                val originalLine = originalLines.getOrNull(i)
                val modifiedLine = modifiedLines.getOrNull(i)
                
                when {
                    originalLine == null -> {
                        differences.add(
                            FileDifference(
                                type = DifferenceType.VALUE_ADDED,
                                location = "Line ${i + 1}",
                                originalValue = null,
                                newValue = modifiedLine,
                                description = "Line ${i + 1} was added"
                            )
                        )
                    }
                    modifiedLine == null -> {
                        differences.add(
                            FileDifference(
                                type = DifferenceType.VALUE_REMOVED,
                                location = "Line ${i + 1}",
                                originalValue = originalLine,
                                newValue = null,
                                description = "Line ${i + 1} was removed"
                            )
                        )
                    }
                    originalLine != modifiedLine -> {
                        differences.add(
                            FileDifference(
                                type = DifferenceType.VALUE_CHANGED,
                                location = "Line ${i + 1}",
                                originalValue = originalLine,
                                newValue = modifiedLine,
                                description = "Line ${i + 1} was modified"
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            differences.add(
                FileDifference(
                    type = DifferenceType.STRUCTURE_CHANGED,
                    location = "File comparison",
                    originalValue = null,
                    newValue = null,
                    description = "Text file comparison failed: ${e.message}"
                )
            )
        }
        
        return differences
    }
    
    private fun isJsonFile(path: String): Boolean {
        return path.lowercase().endsWith(".json")
    }
    
    private fun isPropertiesFile(path: String): Boolean {
        val lower = path.lowercase()
        return lower.endsWith(".properties") || lower.endsWith(".ini") || lower.endsWith(".cfg")
    }
    
    private fun isBinaryFile(path: String): Boolean {
        val lower = path.lowercase()
        return lower.endsWith(".dat") || lower.endsWith(".bin") || lower.endsWith(".sav")
    }
}