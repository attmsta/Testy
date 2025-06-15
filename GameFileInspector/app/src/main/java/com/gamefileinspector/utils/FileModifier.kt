package com.gamefileinspector.utils

import com.gamefileinspector.models.GameFile
import com.gamefileinspector.models.PossibleValue
import com.gamefileinspector.models.DataType
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import org.json.JSONObject
import java.io.File
import java.nio.charset.Charset

object FileModifier {
    
    private val gson = Gson()
    
    fun modifyValue(gameFile: GameFile, possibleValue: PossibleValue, newValue: String): Boolean {
        val file = File(gameFile.path)
        
        if (!file.exists() || !file.canWrite()) {
            return false
        }
        
        return try {
            when {
                possibleValue.description?.startsWith("JSON key:") == true -> {
                    modifyJsonValue(file, possibleValue, newValue)
                }
                possibleValue.description?.startsWith("Key-Value:") == true -> {
                    modifyKeyValuePair(file, possibleValue, newValue)
                }
                possibleValue.description?.contains("binary") == true -> {
                    modifyBinaryValue(file, possibleValue, newValue)
                }
                else -> {
                    modifyTextValue(file, possibleValue, newValue)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    
    private fun modifyJsonValue(file: File, possibleValue: PossibleValue, newValue: String): Boolean {
        val content = file.readText()
        
        try {
            val jsonObject = JSONObject(content)
            val keyName = possibleValue.description?.substringAfter("JSON key: ") ?: return false
            
            // Convert new value to appropriate type
            val convertedValue = when (possibleValue.dataType) {
                DataType.INTEGER, DataType.CURRENCY, DataType.SCORE, DataType.LEVEL, DataType.EXPERIENCE -> {
                    newValue.toIntOrNull() ?: return false
                }
                DataType.FLOAT -> {
                    newValue.toFloatOrNull() ?: return false
                }
                DataType.BOOLEAN -> {
                    newValue.toBooleanStrictOrNull() ?: return false
                }
                else -> newValue
            }
            
            // Update the JSON object
            updateJsonObjectRecursively(jsonObject, keyName, convertedValue)
            
            // Write back to file
            file.writeText(jsonObject.toString(2))
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }
    
    private fun updateJsonObjectRecursively(jsonObject: JSONObject, keyName: String, newValue: Any): Boolean {
        if (jsonObject.has(keyName)) {
            jsonObject.put(keyName, newValue)
            return true
        }
        
        // Search in nested objects
        val keys = jsonObject.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            val value = jsonObject.opt(key)
            if (value is JSONObject) {
                if (updateJsonObjectRecursively(value, keyName, newValue)) {
                    return true
                }
            }
        }
        
        return false
    }
    
    private fun modifyKeyValuePair(file: File, possibleValue: PossibleValue, newValue: String): Boolean {
        val lines = file.readLines().toMutableList()
        val keyName = possibleValue.description?.substringAfter("Key-Value: ") ?: return false
        
        for (i in lines.indices) {
            val line = lines[i]
            if (line.contains("=") && line.startsWith(keyName)) {
                val parts = line.split("=", limit = 2)
                if (parts.size == 2) {
                    lines[i] = "${parts[0]}=$newValue"
                    file.writeText(lines.joinToString("\n"))
                    return true
                }
            } else if (line.contains(":") && line.startsWith(keyName)) {
                val parts = line.split(":", limit = 2)
                if (parts.size == 2) {
                    lines[i] = "${parts[0]}:$newValue"
                    file.writeText(lines.joinToString("\n"))
                    return true
                }
            }
        }
        
        return false
    }
    
    private fun modifyBinaryValue(file: File, possibleValue: PossibleValue, newValue: String): Boolean {
        val content = file.readBytes()
        val offset = possibleValue.offset.toInt()
        
        if (offset < 0 || offset >= content.size - 3) {
            return false
        }
        
        val intValue = newValue.toIntOrNull() ?: return false
        
        // Write 32-bit integer in little-endian format
        content[offset] = (intValue and 0xFF).toByte()
        content[offset + 1] = ((intValue shr 8) and 0xFF).toByte()
        content[offset + 2] = ((intValue shr 16) and 0xFF).toByte()
        content[offset + 3] = ((intValue shr 24) and 0xFF).toByte()
        
        file.writeBytes(content)
        return true
    }
    
    private fun modifyTextValue(file: File, possibleValue: PossibleValue, newValue: String): Boolean {
        val content = file.readText()
        val originalValue = possibleValue.originalValue
        
        // Simple text replacement
        if (content.contains(originalValue)) {
            val newContent = content.replaceFirst(originalValue, newValue)
            file.writeText(newContent)
            return true
        }
        
        return false
    }
}