package com.gamefileinspector.analyzers

import com.gamefileinspector.models.DataType
import com.gamefileinspector.models.GameFile
import com.gamefileinspector.models.PossibleValue
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.regex.Pattern
import kotlin.math.abs

/**
 * Advanced deep analysis for game files with sophisticated value detection
 */
object DeepFileAnalyzer {
    
    // Enhanced game value patterns with context awareness
    private val gameValuePatterns = mapOf(
        "currency" to listOf(
            // Primary currency patterns
            "gold", "coin", "money", "cash", "credits", "dollars", "bucks", "currency",
            // Secondary currency patterns
            "wallet", "balance", "funds", "wealth", "treasure", "loot", "reward",
            // Game-specific currency
            "gil", "zenny", "rupees", "bells", "simoleons", "caps", "souls"
        ),
        "premium_currency" to listOf(
            "gem", "diamond", "crystal", "jewel", "ruby", "emerald", "sapphire",
            "premium", "vip", "elite", "special", "rare", "legendary", "epic",
            "token", "ticket", "voucher", "coupon", "pass", "key"
        ),
        "experience" to listOf(
            "exp", "experience", "xp", "skill", "mastery", "proficiency",
            "knowledge", "wisdom", "learning", "training", "practice",
            "points", "score", "rating", "rank", "grade", "merit"
        ),
        "health_energy" to listOf(
            "health", "hp", "life", "lives", "vitality", "stamina", "energy",
            "power", "fuel", "charge", "battery", "mana", "mp", "spirit",
            "endurance", "vigor", "strength", "force"
        ),
        "progress" to listOf(
            "level", "stage", "tier", "rank", "grade", "class", "division",
            "league", "bracket", "category", "group", "phase", "step",
            "progress", "advancement", "achievement", "milestone", "checkpoint"
        ),
        "inventory" to listOf(
            "item", "weapon", "armor", "tool", "equipment", "gear", "outfit",
            "inventory", "storage", "bag", "backpack", "container", "chest",
            "quantity", "amount", "count", "number", "stack", "pile"
        ),
        "stats" to listOf(
            "attack", "damage", "defense", "armor", "speed", "agility",
            "intelligence", "wisdom", "charisma", "luck", "critical",
            "accuracy", "evasion", "resistance", "immunity", "boost"
        ),
        "time" to listOf(
            "time", "timer", "countdown", "cooldown", "duration", "delay",
            "interval", "period", "session", "playtime", "uptime",
            "timestamp", "date", "schedule", "calendar", "clock"
        ),
        "achievements" to listOf(
            "achievement", "trophy", "medal", "badge", "award", "honor",
            "title", "unlock", "completion", "mastery", "perfect",
            "record", "best", "high", "maximum", "peak", "top"
        )
    )
    
    // Suspicious value ranges that indicate game data
    private val suspiciousRanges = listOf(
        1..100,        // Levels, percentages
        100..1000,     // Medium values
        1000..100000,  // Currency, scores
        100000..10000000 // Large currency, high scores
    )
    
    // Common game file naming patterns
    private val gameFilePatterns = listOf(
        Pattern.compile(".*save.*", Pattern.CASE_INSENSITIVE),
        Pattern.compile(".*player.*", Pattern.CASE_INSENSITIVE),
        Pattern.compile(".*profile.*", Pattern.CASE_INSENSITIVE),
        Pattern.compile(".*progress.*", Pattern.CASE_INSENSITIVE),
        Pattern.compile(".*config.*", Pattern.CASE_INSENSITIVE),
        Pattern.compile(".*settings.*", Pattern.CASE_INSENSITIVE),
        Pattern.compile(".*data.*", Pattern.CASE_INSENSITIVE),
        Pattern.compile(".*game.*", Pattern.CASE_INSENSITIVE),
        Pattern.compile(".*user.*", Pattern.CASE_INSENSITIVE),
        Pattern.compile(".*stats.*", Pattern.CASE_INSENSITIVE),
        Pattern.compile(".*inventory.*", Pattern.CASE_INSENSITIVE),
        Pattern.compile(".*character.*", Pattern.CASE_INSENSITIVE)
    )
    
    /**
     * Perform deep analysis on a game file with enhanced detection
     */
    fun performDeepAnalysis(gameFile: GameFile): List<PossibleValue> {
        val file = File(gameFile.path)
        if (!file.exists() || !file.canRead()) return emptyList()
        
        val detectedValues = mutableListOf<PossibleValue>()
        
        try {
            when {
                gameFile.path.endsWith(".json", true) -> {
                    detectedValues.addAll(analyzeJsonDeep(file))
                }
                gameFile.path.endsWith(".xml", true) -> {
                    detectedValues.addAll(analyzeXmlDeep(file))
                }
                gameFile.path.endsWith(".properties", true) || 
                gameFile.path.endsWith(".ini", true) || 
                gameFile.path.endsWith(".cfg", true) -> {
                    detectedValues.addAll(analyzePropertiesDeep(file))
                }
                gameFile.path.endsWith(".db", true) || 
                gameFile.path.endsWith(".sqlite", true) -> {
                    detectedValues.addAll(analyzeSqliteDeep(file))
                }
                isBinaryFile(file) -> {
                    detectedValues.addAll(analyzeBinaryDeep(file))
                }
                else -> {
                    detectedValues.addAll(analyzeTextDeep(file))
                }
            }
            
            // Apply contextual analysis
            detectedValues.forEach { value ->
                enhanceValueWithContext(value, gameFile, file)
            }
            
            // Filter and rank by confidence
            return detectedValues
                .filter { it.confidence > 0.2 }
                .sortedByDescending { it.confidence }
                .take(100) // Limit to top 100 most confident values
                
        } catch (e: Exception) {
            return emptyList()
        }
    }
    
    /**
     * Deep JSON analysis with nested object traversal
     */
    private fun analyzeJsonDeep(file: File): List<PossibleValue> {
        val values = mutableListOf<PossibleValue>()
        
        try {
            val content = file.readText()
            val json = JSONObject(content)
            
            // Traverse all nested objects and arrays
            traverseJsonObject(json, "", values, content)
            
        } catch (e: Exception) {
            // Fallback to text analysis if JSON parsing fails
            values.addAll(analyzeTextDeep(file))
        }
        
        return values
    }
    
    /**
     * Recursively traverse JSON objects to find all values
     */
    private fun traverseJsonObject(
        obj: JSONObject, 
        path: String, 
        values: MutableList<PossibleValue>,
        originalContent: String
    ) {
        obj.keys().forEach { key ->
            val currentPath = if (path.isEmpty()) key else "$path.$key"
            val value = obj.get(key)
            
            when (value) {
                is JSONObject -> {
                    traverseJsonObject(value, currentPath, values, originalContent)
                }
                is JSONArray -> {
                    traverseJsonArray(value, currentPath, values, originalContent)
                }
                else -> {
                    analyzeJsonValue(key, value, currentPath, values, originalContent)
                }
            }
        }
    }
    
    /**
     * Traverse JSON arrays
     */
    private fun traverseJsonArray(
        array: JSONArray, 
        path: String, 
        values: MutableList<PossibleValue>,
        originalContent: String
    ) {
        for (i in 0 until array.length()) {
            val currentPath = "$path[$i]"
            val value = array.get(i)
            
            when (value) {
                is JSONObject -> {
                    traverseJsonObject(value, currentPath, values, originalContent)
                }
                is JSONArray -> {
                    traverseJsonArray(value, currentPath, values, originalContent)
                }
                else -> {
                    analyzeJsonValue("item_$i", value, currentPath, values, originalContent)
                }
            }
        }
    }
    
    /**
     * Analyze individual JSON values with enhanced pattern matching
     */
    private fun analyzeJsonValue(
        key: String, 
        value: Any, 
        path: String, 
        values: MutableList<PossibleValue>,
        originalContent: String
    ) {
        val confidence = calculateEnhancedConfidence(key, value, path, originalContent)
        
        if (confidence > 0.2) {
            val category = determineValueCategory(key, value, path)
            val dataType = determineDataType(value)
            
            values.add(
                PossibleValue(
                    key = key,
                    value = value.toString(),
                    dataType = dataType,
                    confidence = confidence,
                    description = generateEnhancedDescription(key, value, category, path),
                    location = path,
                    category = category
                )
            )
        }
    }
    
    /**
     * Deep XML analysis with attribute and text content inspection
     */
    private fun analyzeXmlDeep(file: File): List<PossibleValue> {
        val values = mutableListOf<PossibleValue>()
        
        try {
            val content = file.readText()
            
            // Extract XML attributes and text content
            val attributePattern = Pattern.compile("(\\w+)\\s*=\\s*[\"']([^\"']+)[\"']")
            val textPattern = Pattern.compile("<(\\w+)>([^<]+)</\\1>")
            
            // Find attributes
            val attributeMatcher = attributePattern.matcher(content)
            while (attributeMatcher.find()) {
                val attrName = attributeMatcher.group(1)
                val attrValue = attributeMatcher.group(2)
                
                analyzeXmlValue(attrName, attrValue, "attribute", values, content)
            }
            
            // Find text content
            val textMatcher = textPattern.matcher(content)
            while (textMatcher.find()) {
                val tagName = textMatcher.group(1)
                val tagValue = textMatcher.group(2)
                
                analyzeXmlValue(tagName, tagValue, "element", values, content)
            }
            
        } catch (e: Exception) {
            values.addAll(analyzeTextDeep(file))
        }
        
        return values
    }
    
    /**
     * Analyze XML values with context awareness
     */
    private fun analyzeXmlValue(
        name: String, 
        value: String, 
        type: String, 
        values: MutableList<PossibleValue>,
        content: String
    ) {
        // Try to parse as number
        val numericValue = value.toIntOrNull() ?: value.toDoubleOrNull()
        if (numericValue != null) {
            val confidence = calculateEnhancedConfidence(name, numericValue, type, content)
            
            if (confidence > 0.2) {
                val category = determineValueCategory(name, numericValue, type)
                val dataType = if (numericValue is Int) DataType.INTEGER else DataType.FLOAT
                
                values.add(
                    PossibleValue(
                        key = name,
                        value = value,
                        dataType = dataType,
                        confidence = confidence,
                        description = generateEnhancedDescription(name, numericValue, category, type),
                        location = "$type: $name",
                        category = category
                    )
                )
            }
        }
        
        // Check boolean values
        if (value.lowercase() in listOf("true", "false", "yes", "no", "on", "off", "1", "0")) {
            val confidence = calculateEnhancedConfidence(name, value, type, content)
            
            if (confidence > 0.3) {
                values.add(
                    PossibleValue(
                        key = name,
                        value = value,
                        dataType = DataType.BOOLEAN,
                        confidence = confidence,
                        description = "Boolean setting: $name",
                        location = "$type: $name",
                        category = "settings"
                    )
                )
            }
        }
    }
    
    /**
     * Deep properties file analysis with section awareness
     */
    private fun analyzePropertiesDeep(file: File): List<PossibleValue> {
        val values = mutableListOf<PossibleValue>()
        
        try {
            val lines = file.readLines()
            var currentSection = ""
            
            lines.forEachIndexed { lineIndex, line ->
                val trimmedLine = line.trim()
                
                // Detect sections [section_name]
                if (trimmedLine.startsWith("[") && trimmedLine.endsWith("]")) {
                    currentSection = trimmedLine.substring(1, trimmedLine.length - 1)
                    return@forEachIndexed
                }
                
                // Parse key=value pairs
                if (trimmedLine.contains("=") && !trimmedLine.startsWith("#") && !trimmedLine.startsWith(";")) {
                    val parts = trimmedLine.split("=", limit = 2)
                    if (parts.size == 2) {
                        val key = parts[0].trim()
                        val value = parts[1].trim()
                        
                        analyzePropertyValue(key, value, currentSection, lineIndex + 1, values, file.readText())
                    }
                }
            }
            
        } catch (e: Exception) {
            values.addAll(analyzeTextDeep(file))
        }
        
        return values
    }
    
    /**
     * Analyze property values with section context
     */
    private fun analyzePropertyValue(
        key: String, 
        value: String, 
        section: String, 
        lineNumber: Int,
        values: MutableList<PossibleValue>,
        content: String
    ) {
        // Try numeric values
        val numericValue = value.toIntOrNull() ?: value.toDoubleOrNull()
        if (numericValue != null) {
            val fullKey = if (section.isNotEmpty()) "$section.$key" else key
            val confidence = calculateEnhancedConfidence(fullKey, numericValue, section, content)
            
            if (confidence > 0.2) {
                val category = determineValueCategory(fullKey, numericValue, section)
                val dataType = if (numericValue is Int) DataType.INTEGER else DataType.FLOAT
                
                values.add(
                    PossibleValue(
                        key = key,
                        value = value,
                        dataType = dataType,
                        confidence = confidence,
                        description = generateEnhancedDescription(key, numericValue, category, section),
                        location = "Line $lineNumber${if (section.isNotEmpty()) " [$section]" else ""}",
                        category = category
                    )
                )
            }
        }
        
        // Check boolean values
        if (value.lowercase() in listOf("true", "false", "yes", "no", "on", "off", "1", "0", "enabled", "disabled")) {
            val fullKey = if (section.isNotEmpty()) "$section.$key" else key
            val confidence = calculateEnhancedConfidence(fullKey, value, section, content)
            
            if (confidence > 0.3) {
                values.add(
                    PossibleValue(
                        key = key,
                        value = value,
                        dataType = DataType.BOOLEAN,
                        confidence = confidence,
                        description = "Configuration setting: $key",
                        location = "Line $lineNumber${if (section.isNotEmpty()) " [$section]" else ""}",
                        category = "settings"
                    )
                )
            }
        }
    }
    
    /**
     * Deep binary file analysis with multiple data type detection
     */
    private fun analyzeBinaryDeep(file: File): List<PossibleValue> {
        val values = mutableListOf<PossibleValue>()
        
        try {
            val bytes = file.readBytes()
            val buffer = ByteBuffer.wrap(bytes)
            
            // Try both byte orders
            listOf(ByteOrder.LITTLE_ENDIAN, ByteOrder.BIG_ENDIAN).forEach { byteOrder ->
                buffer.order(byteOrder)
                
                // Scan for 32-bit integers
                for (i in 0 until bytes.size - 3 step 1) {
                    buffer.position(i)
                    try {
                        val intValue = buffer.int
                        if (isLikelyGameValue(intValue.toLong())) {
                            val confidence = calculateBinaryConfidence(intValue.toLong(), i, bytes)
                            
                            if (confidence > 0.3) {
                                values.add(
                                    PossibleValue(
                                        key = "int32_${i}_${byteOrder.toString().lowercase()}",
                                        value = intValue.toString(),
                                        dataType = DataType.INTEGER,
                                        confidence = confidence,
                                        description = "32-bit integer at offset $i (${byteOrder})",
                                        location = "Offset $i",
                                        category = "binary_data"
                                    )
                                )
                            }
                        }
                    } catch (e: Exception) {
                        // Continue scanning
                    }
                }
                
                // Scan for 64-bit integers
                for (i in 0 until bytes.size - 7 step 4) {
                    buffer.position(i)
                    try {
                        val longValue = buffer.long
                        if (isLikelyGameValue(longValue)) {
                            val confidence = calculateBinaryConfidence(longValue, i, bytes)
                            
                            if (confidence > 0.3) {
                                values.add(
                                    PossibleValue(
                                        key = "int64_${i}_${byteOrder.toString().lowercase()}",
                                        value = longValue.toString(),
                                        dataType = DataType.INTEGER,
                                        confidence = confidence,
                                        description = "64-bit integer at offset $i (${byteOrder})",
                                        location = "Offset $i",
                                        category = "binary_data"
                                    )
                                )
                            }
                        }
                    } catch (e: Exception) {
                        // Continue scanning
                    }
                }
                
                // Scan for floats
                for (i in 0 until bytes.size - 3 step 2) {
                    buffer.position(i)
                    try {
                        val floatValue = buffer.float
                        if (isLikelyGameFloat(floatValue)) {
                            val confidence = calculateFloatConfidence(floatValue, i, bytes)
                            
                            if (confidence > 0.3) {
                                values.add(
                                    PossibleValue(
                                        key = "float_${i}_${byteOrder.toString().lowercase()}",
                                        value = floatValue.toString(),
                                        dataType = DataType.FLOAT,
                                        confidence = confidence,
                                        description = "Float value at offset $i (${byteOrder})",
                                        location = "Offset $i",
                                        category = "binary_data"
                                    )
                                )
                            }
                        }
                    } catch (e: Exception) {
                        // Continue scanning
                    }
                }
            }
            
            // Look for embedded strings that might contain values
            values.addAll(findEmbeddedStrings(bytes))
            
        } catch (e: Exception) {
            // Fallback to basic analysis
        }
        
        return values.distinctBy { "${it.key}_${it.value}" }
    }
    
    /**
     * Find embedded strings in binary data
     */
    private fun findEmbeddedStrings(bytes: ByteArray): List<PossibleValue> {
        val values = mutableListOf<PossibleValue>()
        val stringBuilder = StringBuilder()
        var stringStart = -1
        
        for (i in bytes.indices) {
            val byte = bytes[i]
            
            if (byte in 32..126) { // Printable ASCII
                if (stringStart == -1) stringStart = i
                stringBuilder.append(byte.toInt().toChar())
            } else {
                if (stringBuilder.length >= 4) { // Minimum string length
                    val foundString = stringBuilder.toString()
                    
                    // Check if string contains key=value patterns
                    if (foundString.contains("=")) {
                        val keyValuePattern = Pattern.compile("(\\w+)=(\\d+(?:\\.\\d+)?)")
                        val matcher = keyValuePattern.matcher(foundString)
                        
                        while (matcher.find()) {
                            val key = matcher.group(1)
                            val value = matcher.group(2)
                            val numValue = value.toDoubleOrNull()
                            
                            if (numValue != null) {
                                val confidence = calculateEnhancedConfidence(key, numValue, "embedded", foundString)
                                
                                if (confidence > 0.4) {
                                    values.add(
                                        PossibleValue(
                                            key = key,
                                            value = value,
                                            dataType = if (value.contains(".")) DataType.FLOAT else DataType.INTEGER,
                                            confidence = confidence,
                                            description = "Embedded value in binary data",
                                            location = "Binary offset $stringStart",
                                            category = determineValueCategory(key, numValue, "embedded")
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
                
                stringBuilder.clear()
                stringStart = -1
            }
        }
        
        return values
    }
    
    /**
     * Enhanced confidence calculation with multiple factors
     */
    private fun calculateEnhancedConfidence(
        key: String, 
        value: Any, 
        context: String, 
        fullContent: String
    ): Double {
        var confidence = 0.0
        val normalizedKey = key.lowercase().replace("_", "").replace("-", "")
        
        // Pattern matching with weights
        gameValuePatterns.forEach { (category, patterns) ->
            patterns.forEach { pattern ->
                if (normalizedKey.contains(pattern)) {
                    confidence += when (category) {
                        "currency" -> 0.9
                        "premium_currency" -> 0.85
                        "experience" -> 0.8
                        "health_energy" -> 0.75
                        "progress" -> 0.7
                        "inventory" -> 0.65
                        "stats" -> 0.6
                        "achievements" -> 0.55
                        "time" -> 0.5
                        else -> 0.3
                    }
                }
            }
        }
        
        // Value range analysis
        if (value is Number) {
            val numValue = value.toDouble()
            
            suspiciousRanges.forEach { range ->
                if (numValue.toInt() in range) {
                    confidence += when (range) {
                        1..100 -> 0.4
                        100..1000 -> 0.5
                        1000..100000 -> 0.6
                        100000..10000000 -> 0.4
                        else -> 0.2
                    }
                }
            }
            
            // Special value patterns
            when {
                numValue == 0.0 -> confidence -= 0.3
                numValue < 0 -> confidence -= 0.4
                numValue.toString().endsWith("000") -> confidence += 0.2 // Round numbers
                numValue in 1.0..10.0 -> confidence += 0.3 // Small integers (levels)
                numValue in 50.0..100.0 -> confidence += 0.2 // Percentages
            }
        }
        
        // Context analysis
        val contextLower = context.lowercase()
        when {
            contextLower.contains("player") -> confidence += 0.2
            contextLower.contains("game") -> confidence += 0.15
            contextLower.contains("save") -> confidence += 0.15
            contextLower.contains("config") -> confidence += 0.1
            contextLower.contains("setting") -> confidence += 0.1
        }
        
        // Content frequency analysis
        val keyOccurrences = fullContent.split(key, ignoreCase = true).size - 1
        when {
            keyOccurrences == 1 -> confidence += 0.1 // Unique occurrence
            keyOccurrences in 2..5 -> confidence += 0.05 // Reasonable frequency
            keyOccurrences > 10 -> confidence -= 0.2 // Too common
        }
        
        return confidence.coerceIn(0.0, 1.0)
    }
    
    /**
     * Determine value category with enhanced logic
     */
    private fun determineValueCategory(key: String, value: Any, context: String): String {
        val normalizedKey = key.lowercase().replace("_", "").replace("-", "")
        
        gameValuePatterns.forEach { (category, patterns) ->
            patterns.forEach { pattern ->
                if (normalizedKey.contains(pattern)) {
                    return category
                }
            }
        }
        
        // Context-based categorization
        val contextLower = context.lowercase()
        when {
            contextLower.contains("inventory") -> return "inventory"
            contextLower.contains("stat") -> return "stats"
            contextLower.contains("setting") -> return "settings"
            contextLower.contains("config") -> return "settings"
            contextLower.contains("audio") -> return "settings"
            contextLower.contains("video") -> return "settings"
            contextLower.contains("graphics") -> return "settings"
        }
        
        // Value-based categorization
        if (value is Number) {
            val numValue = value.toDouble()
            when {
                numValue in 1.0..100.0 -> return "progress"
                numValue in 1000.0..1000000.0 -> return "currency"
                numValue > 1000000.0 -> return "achievements"
            }
        }
        
        return "unknown"
    }
    
    /**
     * Generate enhanced descriptions
     */
    private fun generateEnhancedDescription(
        key: String, 
        value: Any, 
        category: String, 
        context: String
    ): String {
        val baseDesc = when (category) {
            "currency" -> "Game currency"
            "premium_currency" -> "Premium currency"
            "experience" -> "Experience points"
            "health_energy" -> "Health/Energy value"
            "progress" -> "Progress indicator"
            "inventory" -> "Inventory item"
            "stats" -> "Character statistic"
            "achievements" -> "Achievement data"
            "time" -> "Time-related value"
            "settings" -> "Game setting"
            else -> "Possible game value"
        }
        
        val contextInfo = if (context.isNotEmpty() && context != "unknown") " in $context" else ""
        val valueInfo = when (value) {
            is Number -> {
                val num = value.toDouble()
                when {
                    num in 1.0..10.0 -> " (small value, likely level/count)"
                    num in 10.0..100.0 -> " (medium value, likely percentage/level)"
                    num in 100.0..10000.0 -> " (large value, likely currency/score)"
                    num > 10000.0 -> " (very large value, likely high-tier currency/score)"
                    else -> ""
                }
            }
            else -> ""
        }
        
        return "$baseDesc: $key = $value$contextInfo$valueInfo"
    }
    
    /**
     * Enhanced binary confidence calculation
     */
    private fun calculateBinaryConfidence(value: Long, offset: Int, bytes: ByteArray): Double {
        var confidence = 0.0
        
        // Value range analysis
        when {
            value in 1..100 -> confidence += 0.6
            value in 100..10000 -> confidence += 0.7
            value in 10000..1000000 -> confidence += 0.5
            value > 1000000 -> confidence += 0.3
            value == 0L -> confidence -= 0.4
            value < 0 -> confidence -= 0.5
        }
        
        // Position analysis
        when {
            offset % 4 == 0 -> confidence += 0.2 // Aligned to 4-byte boundary
            offset % 8 == 0 -> confidence += 0.3 // Aligned to 8-byte boundary
        }
        
        // Surrounding data analysis
        val surroundingBytes = getSurroundingBytes(bytes, offset, 16)
        val nonZeroCount = surroundingBytes.count { it != 0.toByte() }
        confidence += (nonZeroCount.toDouble() / surroundingBytes.size) * 0.2
        
        return confidence.coerceIn(0.0, 1.0)
    }
    
    /**
     * Calculate confidence for float values
     */
    private fun calculateFloatConfidence(value: Float, offset: Int, bytes: ByteArray): Double {
        var confidence = 0.0
        
        when {
            value in 0.0f..1.0f -> confidence += 0.7 // Likely percentage/ratio
            value in 1.0f..100.0f -> confidence += 0.5 // Reasonable game value
            value in 100.0f..10000.0f -> confidence += 0.4 // Large game value
            value.isNaN() || value.isInfinite() -> confidence -= 0.8
            abs(value) < 0.001f -> confidence -= 0.3 // Very small values
        }
        
        // Position alignment
        if (offset % 4 == 0) confidence += 0.2
        
        return confidence.coerceIn(0.0, 1.0)
    }
    
    /**
     * Check if a float value is likely a game value
     */
    private fun isLikelyGameFloat(value: Float): Boolean {
        return !value.isNaN() && 
               !value.isInfinite() && 
               value >= 0.0f && 
               value <= 1000000.0f
    }
    
    /**
     * Enhanced game value detection for integers
     */
    private fun isLikelyGameValue(value: Long): Boolean {
        return value in 1..100000000 && // Reasonable range
               value != 0L && // Not zero
               value > 0 // Positive values more likely
    }
    
    /**
     * Get surrounding bytes for context analysis
     */
    private fun getSurroundingBytes(bytes: ByteArray, offset: Int, range: Int): ByteArray {
        val start = maxOf(0, offset - range / 2)
        val end = minOf(bytes.size, offset + range / 2)
        return bytes.sliceArray(start until end)
    }
    
    /**
     * Determine data type from value
     */
    private fun determineDataType(value: Any): DataType {
        return when (value) {
            is Int, is Long -> DataType.INTEGER
            is Float, is Double -> DataType.FLOAT
            is Boolean -> DataType.BOOLEAN
            else -> {
                val str = value.toString()
                when {
                    str.toIntOrNull() != null -> DataType.INTEGER
                    str.toDoubleOrNull() != null -> DataType.FLOAT
                    str.lowercase() in listOf("true", "false") -> DataType.BOOLEAN
                    else -> DataType.STRING
                }
            }
        }
    }
    
    /**
     * Check if file is binary
     */
    private fun isBinaryFile(file: File): Boolean {
        try {
            val bytes = file.readBytes().take(1024)
            val nonPrintableCount = bytes.count { it < 32 && it != 9 && it != 10 && it != 13 }
            return nonPrintableCount > bytes.size * 0.3
        } catch (e: Exception) {
            return false
        }
    }
    
    /**
     * Analyze text files with pattern matching
     */
    private fun analyzeTextDeep(file: File): List<PossibleValue> {
        val values = mutableListOf<PossibleValue>()
        
        try {
            val content = file.readText()
            
            // Enhanced key-value pattern matching
            val patterns = listOf(
                Pattern.compile("(\\w+)\\s*[=:]\\s*(\\d+(?:\\.\\d+)?)"),
                Pattern.compile("\"(\\w+)\"\\s*[=:]\\s*(\\d+(?:\\.\\d+)?)"),
                Pattern.compile("'(\\w+)'\\s*[=:]\\s*(\\d+(?:\\.\\d+)?)"),
                Pattern.compile("(\\w+)\\s*=\\s*\"(\\d+(?:\\.\\d+)?)\""),
                Pattern.compile("(\\w+)\\s*=\\s*'(\\d+(?:\\.\\d+)?)'")
            )
            
            patterns.forEach { pattern ->
                val matcher = pattern.matcher(content)
                while (matcher.find()) {
                    val key = matcher.group(1)
                    val valueStr = matcher.group(2)
                    val value = valueStr.toDoubleOrNull()
                    
                    if (value != null) {
                        val confidence = calculateEnhancedConfidence(key, value, "text", content)
                        
                        if (confidence > 0.3) {
                            values.add(
                                PossibleValue(
                                    key = key,
                                    value = valueStr,
                                    dataType = if (valueStr.contains(".")) DataType.FLOAT else DataType.INTEGER,
                                    confidence = confidence,
                                    description = generateEnhancedDescription(key, value, determineValueCategory(key, value, "text"), "text"),
                                    location = "Text pattern match",
                                    category = determineValueCategory(key, value, "text")
                                )
                            )
                        }
                    }
                }
            }
            
        } catch (e: Exception) {
            // Silent failure
        }
        
        return values
    }
    
    /**
     * Enhance value with additional context
     */
    private fun enhanceValueWithContext(value: PossibleValue, gameFile: GameFile, file: File) {
        // Add file-based context
        val fileName = file.name.lowercase()
        gameFilePatterns.forEach { pattern ->
            if (pattern.matcher(fileName).matches()) {
                // Boost confidence for values in files with game-related names
                value.confidence = minOf(1.0, value.confidence + 0.1)
            }
        }
        
        // Add package-based context
        if (gameFile.gamePackage.contains("game", ignoreCase = true)) {
            value.confidence = minOf(1.0, value.confidence + 0.05)
        }
    }
    
    /**
     * Analyze SQLite database files
     */
    private fun analyzeSqliteDeep(file: File): List<PossibleValue> {
        val values = mutableListOf<PossibleValue>()
        
        try {
            // Basic SQLite header check
            val header = file.readBytes().take(16)
            if (header.take(6).map { it.toInt().toChar() }.joinToString("") == "SQLite") {
                // This is a SQLite file, but we'll do basic binary analysis
                // since we don't have SQLite library access
                values.addAll(analyzeBinaryDeep(file))
            }
        } catch (e: Exception) {
            // Fallback to binary analysis
            values.addAll(analyzeBinaryDeep(file))
        }
        
        return values
    }
}