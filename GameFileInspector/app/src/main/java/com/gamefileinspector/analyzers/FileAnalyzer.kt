package com.gamefileinspector.analyzers

import com.gamefileinspector.models.*
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import org.json.JSONObject
import org.json.JSONException
import java.io.File
import java.nio.charset.Charset
import java.util.regex.Pattern

class FileAnalyzer {
    
    private val gson = Gson()
    
    fun analyzeFile(gameFile: GameFile): FileAnalysis {
        val file = File(gameFile.path)
        
        if (!file.exists() || !file.canRead()) {
            return FileAnalysis(
                file = gameFile,
                encoding = null,
                structure = FileStructure.Binary,
                possibleValues = emptyList()
            )
        }
        
        val content = try {
            file.readBytes()
        } catch (e: Exception) {
            return FileAnalysis(
                file = gameFile,
                encoding = null,
                structure = FileStructure.Binary,
                possibleValues = emptyList()
            )
        }
        
        val encoding = detectEncoding(content)
        val structure = analyzeStructure(content, encoding)
        val possibleValues = findPossibleGameValues(content, structure, encoding)
        
        return FileAnalysis(
            file = gameFile,
            encoding = encoding,
            structure = structure,
            possibleValues = possibleValues
        )
    }
    
    private fun detectEncoding(content: ByteArray): String? {
        // Try to detect if content is text
        val charsets = listOf(
            Charsets.UTF_8,
            Charsets.UTF_16,
            Charsets.ISO_8859_1,
            Charsets.US_ASCII
        )
        
        for (charset in charsets) {
            try {
                val text = String(content, charset)
                if (isPrintableText(text)) {
                    return charset.name()
                }
            } catch (e: Exception) {
                continue
            }
        }
        
        return null
    }
    
    private fun isPrintableText(text: String): Boolean {
        val printableRatio = text.count { it.isLetterOrDigit() || it.isWhitespace() || ".,;:!?()[]{}\"'-_=+*&%$#@".contains(it) }.toFloat() / text.length
        return printableRatio > 0.7 && text.length > 0
    }
    
    private fun analyzeStructure(content: ByteArray, encoding: String?): FileStructure {
        if (encoding == null) {
            return FileStructure.Binary
        }
        
        val text = String(content, Charset.forName(encoding))
        
        // Check for JSON
        if (isJsonFormat(text)) {
            val keys = extractJsonKeys(text)
            return FileStructure.Json(keys)
        }
        
        // Check for XML
        if (isXmlFormat(text)) {
            val elements = extractXmlElements(text)
            return FileStructure.Xml(elements)
        }
        
        // Check for key-value pairs
        val keyValuePairs = extractKeyValuePairs(text)
        if (keyValuePairs.isNotEmpty()) {
            return FileStructure.KeyValue(keyValuePairs)
        }
        
        return FileStructure.PlainText
    }
    
    private fun isJsonFormat(text: String): Boolean {
        return try {
            JSONObject(text.trim())
            true
        } catch (e: JSONException) {
            try {
                gson.fromJson(text.trim(), Any::class.java)
                true
            } catch (e: JsonSyntaxException) {
                false
            }
        }
    }
    
    private fun isXmlFormat(text: String): Boolean {
        val trimmed = text.trim()
        return trimmed.startsWith("<?xml") || 
               (trimmed.startsWith("<") && trimmed.endsWith(">") && trimmed.contains("</"))
    }
    
    private fun extractJsonKeys(text: String): List<String> {
        val keys = mutableSetOf<String>()
        try {
            val jsonObject = JSONObject(text)
            extractJsonKeysRecursive(jsonObject, keys)
        } catch (e: Exception) {
            // Try with Gson as fallback
            try {
                val map = gson.fromJson(text, Map::class.java)
                extractMapKeys(map, keys)
            } catch (e: Exception) {
                // Ignore
            }
        }
        return keys.toList()
    }
    
    private fun extractJsonKeysRecursive(jsonObject: JSONObject, keys: MutableSet<String>) {
        val iterator = jsonObject.keys()
        while (iterator.hasNext()) {
            val key = iterator.next()
            keys.add(key)
            try {
                val value = jsonObject.get(key)
                if (value is JSONObject) {
                    extractJsonKeysRecursive(value, keys)
                }
            } catch (e: Exception) {
                // Ignore
            }
        }
    }
    
    private fun extractMapKeys(map: Map<*, *>, keys: MutableSet<String>) {
        map.keys.forEach { key ->
            if (key is String) {
                keys.add(key)
            }
            val value = map[key]
            if (value is Map<*, *>) {
                extractMapKeys(value, keys)
            }
        }
    }
    
    private fun extractXmlElements(text: String): List<String> {
        val elements = mutableSetOf<String>()
        val pattern = Pattern.compile("<([^/!?][^>\\s]*)(?:\\s[^>]*)?>")
        val matcher = pattern.matcher(text)
        
        while (matcher.find()) {
            elements.add(matcher.group(1))
        }
        
        return elements.toList()
    }
    
    private fun extractKeyValuePairs(text: String): Map<String, String> {
        val pairs = mutableMapOf<String, String>()
        val lines = text.lines()
        
        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.contains("=")) {
                val parts = trimmed.split("=", limit = 2)
                if (parts.size == 2) {
                    pairs[parts[0].trim()] = parts[1].trim()
                }
            } else if (trimmed.contains(":")) {
                val parts = trimmed.split(":", limit = 2)
                if (parts.size == 2) {
                    pairs[parts[0].trim()] = parts[1].trim()
                }
            }
        }
        
        return pairs
    }
    
    private fun findPossibleGameValues(content: ByteArray, structure: FileStructure, encoding: String?): List<PossibleValue> {
        val possibleValues = mutableListOf<PossibleValue>()
        
        when (structure) {
            is FileStructure.Json -> {
                possibleValues.addAll(findJsonGameValues(content, encoding))
            }
            is FileStructure.KeyValue -> {
                possibleValues.addAll(findKeyValueGameValues(structure.pairs))
            }
            is FileStructure.PlainText -> {
                possibleValues.addAll(findTextGameValues(content, encoding))
            }
            is FileStructure.Binary -> {
                possibleValues.addAll(findBinaryGameValues(content))
            }
            else -> {
                // Handle other structures
            }
        }
        
        return possibleValues.sortedByDescending { it.confidence }
    }
    
    private fun findJsonGameValues(content: ByteArray, encoding: String?): List<PossibleValue> {
        if (encoding == null) return emptyList()
        
        val possibleValues = mutableListOf<PossibleValue>()
        val text = String(content, Charset.forName(encoding))
        
        try {
            val jsonObject = JSONObject(text)
            findJsonGameValuesRecursive(jsonObject, possibleValues, 0)
        } catch (e: Exception) {
            // Ignore
        }
        
        return possibleValues
    }
    
    private fun findJsonGameValuesRecursive(jsonObject: JSONObject, possibleValues: MutableList<PossibleValue>, baseOffset: Long) {
        val gameKeys = listOf(
            "gold", "coin", "coins", "money", "cash", "currency",
            "score", "points", "exp", "experience", "xp",
            "level", "stage", "lives", "health", "hp",
            "gems", "diamonds", "crystals", "energy"
        )
        
        val iterator = jsonObject.keys()
        while (iterator.hasNext()) {
            val key = iterator.next()
            try {
                val value = jsonObject.get(key)
                
                val keyLower = key.lowercase()
                val matchingKey = gameKeys.find { keyLower.contains(it) }
                
                if (matchingKey != null && (value is Number || value is String)) {
                    val dataType = when (matchingKey) {
                        in listOf("gold", "coin", "coins", "money", "cash", "currency", "gems", "diamonds", "crystals") -> DataType.CURRENCY
                        in listOf("score", "points") -> DataType.SCORE
                        in listOf("exp", "experience", "xp") -> DataType.EXPERIENCE
                        "level", "stage" -> DataType.LEVEL
                        else -> DataType.INTEGER
                    }
                    
                    possibleValues.add(
                        PossibleValue(
                            offset = baseOffset,
                            originalValue = value.toString(),
                            dataType = dataType,
                            description = "JSON key: $key",
                            confidence = 0.9f
                        )
                    )
                }
                
                if (value is JSONObject) {
                    findJsonGameValuesRecursive(value, possibleValues, baseOffset)
                }
            } catch (e: Exception) {
                // Ignore
            }
        }
    }
    
    private fun findKeyValueGameValues(pairs: Map<String, String>): List<PossibleValue> {
        val possibleValues = mutableListOf<PossibleValue>()
        val gameKeys = listOf(
            "gold", "coin", "coins", "money", "cash", "currency",
            "score", "points", "exp", "experience", "xp",
            "level", "stage", "lives", "health", "hp",
            "gems", "diamonds", "crystals", "energy"
        )
        
        pairs.forEach { (key, value) ->
            val keyLower = key.lowercase()
            val matchingKey = gameKeys.find { keyLower.contains(it) }
            
            if (matchingKey != null) {
                val dataType = when (matchingKey) {
                    in listOf("gold", "coin", "coins", "money", "cash", "currency", "gems", "diamonds", "crystals") -> DataType.CURRENCY
                    in listOf("score", "points") -> DataType.SCORE
                    in listOf("exp", "experience", "xp") -> DataType.EXPERIENCE
                    "level", "stage" -> DataType.LEVEL
                    else -> DataType.INTEGER
                }
                
                possibleValues.add(
                    PossibleValue(
                        offset = 0,
                        originalValue = value,
                        dataType = dataType,
                        description = "Key-Value: $key",
                        confidence = 0.8f
                    )
                )
            }
        }
        
        return possibleValues
    }
    
    private fun findTextGameValues(content: ByteArray, encoding: String?): List<PossibleValue> {
        if (encoding == null) return emptyList()
        
        val possibleValues = mutableListOf<PossibleValue>()
        val text = String(content, Charset.forName(encoding))
        
        // Look for numeric values that might be game-related
        val numberPattern = Pattern.compile("\\b\\d+\\b")
        val matcher = numberPattern.matcher(text)
        
        while (matcher.find()) {
            val value = matcher.group()
            val number = value.toIntOrNull()
            
            if (number != null && number > 0 && number < 1000000) {
                possibleValues.add(
                    PossibleValue(
                        offset = matcher.start().toLong(),
                        originalValue = value,
                        dataType = DataType.INTEGER,
                        description = "Numeric value in text",
                        confidence = 0.3f
                    )
                )
            }
        }
        
        return possibleValues
    }
    
    private fun findBinaryGameValues(content: ByteArray): List<PossibleValue> {
        val possibleValues = mutableListOf<PossibleValue>()
        
        // Look for 32-bit integers in binary data
        for (i in 0 until content.size - 3 step 4) {
            try {
                val value = ((content[i].toInt() and 0xFF) shl 24) or
                           ((content[i + 1].toInt() and 0xFF) shl 16) or
                           ((content[i + 2].toInt() and 0xFF) shl 8) or
                           (content[i + 3].toInt() and 0xFF)
                
                if (value > 0 && value < 1000000) {
                    possibleValues.add(
                        PossibleValue(
                            offset = i.toLong(),
                            originalValue = value.toString(),
                            dataType = DataType.INTEGER,
                            description = "32-bit integer in binary",
                            confidence = 0.2f
                        )
                    )
                }
            } catch (e: Exception) {
                // Ignore
            }
        }
        
        return possibleValues
    }
}