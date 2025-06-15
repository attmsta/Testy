package com.gamefileinspector.utils

import com.gamefileinspector.models.GameFile
import com.gamefileinspector.models.PossibleValue
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * Utility for exporting and importing file modifications
 */
object ModificationExporter {
    
    data class ModificationProfile(
        val name: String,
        val description: String,
        val gamePackage: String,
        val targetFile: String,
        val modifications: List<Modification>,
        val createdDate: String,
        val version: String = "1.0"
    )
    
    data class Modification(
        val key: String,
        val originalValue: String,
        val newValue: String,
        val dataType: String,
        val location: String,
        val description: String
    )
    
    /**
     * Exports modifications to a JSON file
     */
    fun exportModifications(
        gameFile: GameFile,
        modifications: List<PossibleValue>,
        profileName: String,
        description: String,
        outputPath: String
    ): Boolean {
        try {
            val profile = ModificationProfile(
                name = profileName,
                description = description,
                gamePackage = gameFile.gamePackage,
                targetFile = File(gameFile.path).name,
                modifications = modifications.map { value ->
                    Modification(
                        key = value.key,
                        originalValue = value.originalValue ?: value.value,
                        newValue = value.value,
                        dataType = value.dataType.name,
                        location = value.location,
                        description = value.description
                    )
                },
                createdDate = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
            )
            
            val json = profileToJson(profile)
            File(outputPath).writeText(json.toString(2))
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }
    
    /**
     * Imports modifications from a JSON file
     */
    fun importModifications(filePath: String): ModificationProfile? {
        try {
            val jsonContent = File(filePath).readText()
            val json = JSONObject(jsonContent)
            return jsonToProfile(json)
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }
    
    /**
     * Applies imported modifications to a game file
     */
    fun applyModifications(
        gameFile: GameFile,
        profile: ModificationProfile,
        createBackup: Boolean = true
    ): Boolean {
        try {
            // Verify compatibility
            if (!isCompatible(gameFile, profile)) {
                return false
            }
            
            // Create backup if requested
            if (createBackup) {
                FileBackupManager.createBackup(gameFile)
            }
            
            // Apply modifications
            profile.modifications.forEach { modification ->
                val possibleValue = PossibleValue(
                    key = modification.key,
                    value = modification.newValue,
                    dataType = com.gamefileinspector.models.DataType.valueOf(modification.dataType),
                    confidence = 1.0,
                    description = modification.description,
                    location = modification.location,
                    category = "imported"
                )
                
                FileModifier.modifyValue(gameFile, possibleValue)
            }
            
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }
    
    /**
     * Creates a modification template for a game file
     */
    fun createTemplate(gameFile: GameFile, detectedValues: List<PossibleValue>): String {
        val template = JSONObject().apply {
            put("name", "Modification Template for ${File(gameFile.path).name}")
            put("description", "Template for modifying ${gameFile.gamePackage}")
            put("gamePackage", gameFile.gamePackage)
            put("targetFile", File(gameFile.path).name)
            put("version", "1.0")
            put("createdDate", SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date()))
            
            val modificationsArray = JSONArray()
            detectedValues.forEach { value ->
                val modificationTemplate = JSONObject().apply {
                    put("key", value.key)
                    put("originalValue", value.value)
                    put("newValue", "CHANGE_THIS_VALUE")
                    put("dataType", value.dataType.name)
                    put("location", value.location)
                    put("description", value.description)
                    put("confidence", value.confidence)
                    put("category", value.category)
                }
                modificationsArray.put(modificationTemplate)
            }
            put("modifications", modificationsArray)
        }
        
        return template.toString(2)
    }
    
    /**
     * Validates a modification profile
     */
    fun validateProfile(profile: ModificationProfile): List<String> {
        val errors = mutableListOf<String>()
        
        if (profile.name.isBlank()) {
            errors.add("Profile name cannot be empty")
        }
        
        if (profile.gamePackage.isBlank()) {
            errors.add("Game package cannot be empty")
        }
        
        if (profile.targetFile.isBlank()) {
            errors.add("Target file cannot be empty")
        }
        
        if (profile.modifications.isEmpty()) {
            errors.add("No modifications specified")
        }
        
        profile.modifications.forEachIndexed { index, modification ->
            if (modification.key.isBlank()) {
                errors.add("Modification $index: Key cannot be empty")
            }
            
            if (modification.newValue.isBlank()) {
                errors.add("Modification $index: New value cannot be empty")
            }
            
            try {
                com.gamefileinspector.models.DataType.valueOf(modification.dataType)
            } catch (e: IllegalArgumentException) {
                errors.add("Modification $index: Invalid data type '${modification.dataType}'")
            }
        }
        
        return errors
    }
    
    /**
     * Lists available modification profiles in a directory
     */
    fun listProfiles(directory: String): List<ModificationProfile> {
        val profiles = mutableListOf<ModificationProfile>()
        
        try {
            val dir = File(directory)
            if (dir.exists() && dir.isDirectory) {
                dir.listFiles { file -> file.extension.lowercase() == "json" }?.forEach { file ->
                    importModifications(file.absolutePath)?.let { profile ->
                        profiles.add(profile)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        return profiles
    }
    
    /**
     * Exports multiple profiles as a collection
     */
    fun exportProfileCollection(
        profiles: List<ModificationProfile>,
        collectionName: String,
        outputPath: String
    ): Boolean {
        try {
            val collection = JSONObject().apply {
                put("name", collectionName)
                put("description", "Collection of modification profiles")
                put("version", "1.0")
                put("createdDate", SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date()))
                put("profileCount", profiles.size)
                
                val profilesArray = JSONArray()
                profiles.forEach { profile ->
                    profilesArray.put(profileToJson(profile))
                }
                put("profiles", profilesArray)
            }
            
            File(outputPath).writeText(collection.toString(2))
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }
    
    private fun profileToJson(profile: ModificationProfile): JSONObject {
        return JSONObject().apply {
            put("name", profile.name)
            put("description", profile.description)
            put("gamePackage", profile.gamePackage)
            put("targetFile", profile.targetFile)
            put("version", profile.version)
            put("createdDate", profile.createdDate)
            
            val modificationsArray = JSONArray()
            profile.modifications.forEach { modification ->
                val modificationJson = JSONObject().apply {
                    put("key", modification.key)
                    put("originalValue", modification.originalValue)
                    put("newValue", modification.newValue)
                    put("dataType", modification.dataType)
                    put("location", modification.location)
                    put("description", modification.description)
                }
                modificationsArray.put(modificationJson)
            }
            put("modifications", modificationsArray)
        }
    }
    
    private fun jsonToProfile(json: JSONObject): ModificationProfile {
        val modificationsArray = json.getJSONArray("modifications")
        val modifications = mutableListOf<Modification>()
        
        for (i in 0 until modificationsArray.length()) {
            val modificationJson = modificationsArray.getJSONObject(i)
            modifications.add(
                Modification(
                    key = modificationJson.getString("key"),
                    originalValue = modificationJson.getString("originalValue"),
                    newValue = modificationJson.getString("newValue"),
                    dataType = modificationJson.getString("dataType"),
                    location = modificationJson.getString("location"),
                    description = modificationJson.getString("description")
                )
            )
        }
        
        return ModificationProfile(
            name = json.getString("name"),
            description = json.getString("description"),
            gamePackage = json.getString("gamePackage"),
            targetFile = json.getString("targetFile"),
            modifications = modifications,
            createdDate = json.getString("createdDate"),
            version = json.optString("version", "1.0")
        )
    }
    
    private fun isCompatible(gameFile: GameFile, profile: ModificationProfile): Boolean {
        // Check if game package matches
        if (gameFile.gamePackage != profile.gamePackage) {
            return false
        }
        
        // Check if target file name matches
        val fileName = File(gameFile.path).name
        if (fileName != profile.targetFile) {
            return false
        }
        
        return true
    }
}