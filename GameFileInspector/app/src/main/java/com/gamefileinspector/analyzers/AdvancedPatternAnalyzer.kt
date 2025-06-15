package com.gamefileinspector.analyzers

import com.gamefileinspector.models.DataType
import com.gamefileinspector.models.PossibleValue
import java.util.regex.Pattern

/**
 * Advanced pattern analyzer for detecting game values with sophisticated heuristics
 */
object AdvancedPatternAnalyzer {
    
    // Currency patterns with context
    private val currencyPatterns = mapOf(
        "gold" to listOf("gold", "coin", "money", "cash", "currency", "credits"),
        "gems" to listOf("gem", "diamond", "crystal", "jewel", "premium"),
        "energy" to listOf("energy", "stamina", "power", "fuel", "charge"),
        "experience" to listOf("exp", "experience", "xp", "skill_points", "sp")
    )
    
    // Level/Progress patterns
    private val progressPatterns = listOf(
        "level", "stage", "rank", "tier", "grade", "progress", "completion"
    )
    
    // Score patterns
    private val scorePatterns = listOf(
        "score", "points", "highscore", "best", "record", "achievement"
    )
    
    // Health/Combat patterns
    private val combatPatterns = listOf(
        "health", "hp", "life", "lives", "damage", "attack", "defense", "armor"
    )
    
    // Time patterns
    private val timePatterns = listOf(
        "time", "duration", "cooldown", "timer", "delay", "interval"
    )
    
    /**
     * Analyzes a key-value pair to determine if it's likely a game value
     */
    fun analyzeKeyValue(key: String, value: Any, context: String = ""): PossibleValue? {
        val normalizedKey = key.lowercase().replace("_", "").replace("-", "")
        val confidence = calculateConfidence(normalizedKey, value, context)
        
        if (confidence < 0.3) return null
        
        val category = determineCategory(normalizedKey)
        val dataType = determineDataType(value)
        
        return PossibleValue(
            key = key,
            value = value.toString(),
            dataType = dataType,
            confidence = confidence,
            description = generateDescription(category, key, value),
            location = "Key: $key",
            category = category
        )
    }
    
    /**
     * Analyzes binary data for potential game values
     */
    fun analyzeBinaryData(data: ByteArray, offset: Int = 0): List<PossibleValue> {
        val values = mutableListOf<PossibleValue>()
        
        // Look for 32-bit integers that might be game values
        for (i in 0 until data.size - 3 step 4) {
            val intValue = bytesToInt(data, i)
            if (isLikelyGameValue(intValue)) {
                values.add(
                    PossibleValue(
                        key = "int_${offset + i}",
                        value = intValue.toString(),
                        dataType = DataType.INTEGER,
                        confidence = calculateBinaryConfidence(intValue),
                        description = "Possible game value at offset ${offset + i}",
                        location = "Offset: ${offset + i}",
                        category = "unknown"
                    )
                )
            }
        }
        
        // Look for 64-bit integers
        for (i in 0 until data.size - 7 step 8) {
            val longValue = bytesToLong(data, i)
            if (isLikelyGameValue(longValue)) {
                values.add(
                    PossibleValue(
                        key = "long_${offset + i}",
                        value = longValue.toString(),
                        dataType = DataType.INTEGER,
                        confidence = calculateBinaryConfidence(longValue),
                        description = "Possible large game value at offset ${offset + i}",
                        location = "Offset: ${offset + i}",
                        category = "unknown"
                    )
                )
            }
        }
        
        return values
    }
    
    /**
     * Analyzes text content for embedded values
     */
    fun analyzeTextContent(content: String): List<PossibleValue> {
        val values = mutableListOf<PossibleValue>()
        
        // Look for key=value patterns
        val keyValuePattern = Pattern.compile("([a-zA-Z_][a-zA-Z0-9_]*)[\\s]*[=:][\\s]*([0-9]+(?:\\.[0-9]+)?)")
        val matcher = keyValuePattern.matcher(content)
        
        while (matcher.find()) {
            val key = matcher.group(1)
            val valueStr = matcher.group(2)
            val value = if (valueStr.contains(".")) valueStr.toDoubleOrNull() else valueStr.toLongOrNull()
            
            value?.let {
                analyzeKeyValue(key, it, content)?.let { possibleValue ->
                    values.add(possibleValue.copy(location = "Line: ${getLineNumber(content, matcher.start())}"))
                }
            }
        }
        
        return values
    }
    
    private fun calculateConfidence(key: String, value: Any, context: String): Double {
        var confidence = 0.0
        
        // Check against known patterns
        currencyPatterns.forEach { (category, patterns) ->
            patterns.forEach { pattern ->
                if (key.contains(pattern)) {
                    confidence += 0.8
                    return@forEach
                }
            }
        }
        
        progressPatterns.forEach { pattern ->
            if (key.contains(pattern)) {
                confidence += 0.7
            }
        }
        
        scorePatterns.forEach { pattern ->
            if (key.contains(pattern)) {
                confidence += 0.6
            }
        }
        
        combatPatterns.forEach { pattern ->
            if (key.contains(pattern)) {
                confidence += 0.5
            }
        }
        
        timePatterns.forEach { pattern ->
            if (key.contains(pattern)) {
                confidence += 0.4
            }
        }
        
        // Value-based confidence adjustments
        when (value) {
            is Number -> {
                val numValue = value.toDouble()
                when {
                    numValue in 1.0..1000.0 -> confidence += 0.3 // Likely level/small currency
                    numValue in 1000.0..1000000.0 -> confidence += 0.4 // Likely currency/score
                    numValue > 1000000.0 -> confidence += 0.2 // Possibly score
                    numValue == 0.0 -> confidence -= 0.2 // Less likely to be important
                    numValue < 0.0 -> confidence -= 0.3 // Negative values less likely
                }
            }
            is Boolean -> confidence += 0.2 // Boolean settings
        }
        
        // Context-based adjustments
        if (context.contains("player", ignoreCase = true)) confidence += 0.1
        if (context.contains("game", ignoreCase = true)) confidence += 0.1
        if (context.contains("save", ignoreCase = true)) confidence += 0.1
        
        return confidence.coerceIn(0.0, 1.0)
    }
    
    private fun determineCategory(key: String): String {
        currencyPatterns.forEach { (category, patterns) ->
            patterns.forEach { pattern ->
                if (key.contains(pattern)) return category
            }
        }
        
        when {
            progressPatterns.any { key.contains(it) } -> return "progress"
            scorePatterns.any { key.contains(it) } -> return "score"
            combatPatterns.any { key.contains(it) } -> return "combat"
            timePatterns.any { key.contains(it) } -> return "time"
            else -> return "unknown"
        }
    }
    
    private fun determineDataType(value: Any): DataType {
        return when (value) {
            is Int, is Long -> DataType.INTEGER
            is Float, is Double -> DataType.FLOAT
            is Boolean -> DataType.BOOLEAN
            else -> DataType.STRING
        }
    }
    
    private fun generateDescription(category: String, key: String, value: Any): String {
        val baseDesc = when (category) {
            "gold" -> "Currency value"
            "gems" -> "Premium currency"
            "energy" -> "Energy/stamina value"
            "experience" -> "Experience points"
            "progress" -> "Progress/level value"
            "score" -> "Score/points value"
            "combat" -> "Combat-related value"
            "time" -> "Time-related value"
            else -> "Possible game value"
        }
        
        return "$baseDesc: $key = $value"
    }
    
    private fun isLikelyGameValue(value: Long): Boolean {
        return when {
            value in 1..1000000 -> true // Reasonable range for most game values
            value == 0L -> false // Zero values less interesting
            value < 0 -> false // Negative values less likely
            value > 1000000000 -> false // Too large to be typical game value
            else -> false
        }
    }
    
    private fun calculateBinaryConfidence(value: Long): Double {
        return when {
            value in 1..100 -> 0.7 // Likely level/small count
            value in 100..10000 -> 0.8 // Likely currency/score
            value in 10000..1000000 -> 0.6 // Possibly large currency
            else -> 0.3
        }
    }
    
    private fun bytesToInt(bytes: ByteArray, offset: Int): Int {
        return ((bytes[offset].toInt() and 0xFF) shl 24) or
                ((bytes[offset + 1].toInt() and 0xFF) shl 16) or
                ((bytes[offset + 2].toInt() and 0xFF) shl 8) or
                (bytes[offset + 3].toInt() and 0xFF)
    }
    
    private fun bytesToLong(bytes: ByteArray, offset: Int): Long {
        var result = 0L
        for (i in 0..7) {
            result = (result shl 8) or (bytes[offset + i].toLong() and 0xFF)
        }
        return result
    }
    
    private fun getLineNumber(content: String, position: Int): Int {
        return content.substring(0, position).count { it == '\n' } + 1
    }
}