package com.gamefileinspector.analyzers

import com.gamefileinspector.models.DataType
import com.gamefileinspector.models.FileStructure
import com.gamefileinspector.models.PossibleValue
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.regex.Pattern
import kotlin.experimental.and

/**
 * Advanced file structure analyzer for understanding complex game file formats
 */
object FileStructureAnalyzer {
    
    // Known game file signatures
    private val gameFileSignatures = mapOf(
        "SAVE" to "Generic save file",
        "GAME" to "Game data file",
        "PLYR" to "Player data file",
        "PROF" to "Profile data file",
        "PROG" to "Progress data file",
        "INVT" to "Inventory data file",
        "STAT" to "Statistics file",
        "CONF" to "Configuration file",
        "PREF" to "Preferences file",
        "ACHV" to "Achievement file",
        "SCOR" to "Score file",
        "LEVL" to "Level data file",
        "WRLD" to "World data file",
        "CHAR" to "Character data file",
        "ITEM" to "Item data file",
        "QUES" to "Quest data file",
        "SKIL" to "Skill data file",
        "EQUP" to "Equipment data file",
        "CURR" to "Currency data file",
        "TIME" to "Time data file"
    )
    
    // Binary data type patterns
    private val binaryPatterns = mapOf(
        "int32_le" to { buffer: ByteBuffer, offset: Int ->
            buffer.order(ByteOrder.LITTLE_ENDIAN)
            buffer.position(offset)
            buffer.int
        },
        "int32_be" to { buffer: ByteBuffer, offset: Int ->
            buffer.order(ByteOrder.BIG_ENDIAN)
            buffer.position(offset)
            buffer.int
        },
        "int64_le" to { buffer: ByteBuffer, offset: Int ->
            buffer.order(ByteOrder.LITTLE_ENDIAN)
            buffer.position(offset)
            buffer.long
        },
        "int64_be" to { buffer: ByteBuffer, offset: Int ->
            buffer.order(ByteOrder.BIG_ENDIAN)
            buffer.position(offset)
            buffer.long
        },
        "float_le" to { buffer: ByteBuffer, offset: Int ->
            buffer.order(ByteOrder.LITTLE_ENDIAN)
            buffer.position(offset)
            buffer.float
        },
        "float_be" to { buffer: ByteBuffer, offset: Int ->
            buffer.order(ByteOrder.BIG_ENDIAN)
            buffer.position(offset)
            buffer.float
        },
        "double_le" to { buffer: ByteBuffer, offset: Int ->
            buffer.order(ByteOrder.LITTLE_ENDIAN)
            buffer.position(offset)
            buffer.double
        },
        "double_be" to { buffer: ByteBuffer, offset: Int ->
            buffer.order(ByteOrder.BIG_ENDIAN)
            buffer.position(offset)
            buffer.double
        }
    )
    
    // Common game data structures
    data class GameDataStructure(
        val name: String,
        val pattern: ByteArray,
        val description: String,
        val valueExtractor: (ByteArray, Int) -> List<PossibleValue>
    )
    
    /**
     * Analyze file structure and extract values based on detected format
     */
    fun analyzeFileStructure(file: File): List<PossibleValue> {
        val values = mutableListOf<PossibleValue>()
        
        if (!file.exists() || !file.canRead()) return values
        
        try {
            val bytes = file.readBytes()
            val structure = detectFileStructure(bytes)
            
            when (structure) {
                FileStructure.BINARY -> values.addAll(analyzeBinaryStructure(bytes, file))
                FileStructure.JSON -> values.addAll(analyzeJsonStructure(file))
                FileStructure.XML -> values.addAll(analyzeXmlStructure(file))
                FileStructure.KEY_VALUE -> values.addAll(analyzeKeyValueStructure(file))
                FileStructure.DATABASE -> values.addAll(analyzeDatabaseStructure(bytes, file))
                FileStructure.TEXT -> values.addAll(analyzeTextStructure(file))
                else -> values.addAll(analyzeUnknownStructure(bytes, file))
            }
            
        } catch (e: Exception) {
            // Fallback to basic analysis
            values.addAll(analyzeBasicStructure(file))
        }
        
        return values.distinctBy { "${it.key}_${it.value}_${it.location}" }
            .sortedByDescending { it.confidence }
    }
    
    /**
     * Detect file structure from binary content
     */
    private fun detectFileStructure(bytes: ByteArray): FileStructure {
        if (bytes.isEmpty()) return FileStructure.UNKNOWN
        
        // Check for text-based formats first
        val textContent = try {
            String(bytes.take(1024).toByteArray(), Charsets.UTF_8)
        } catch (e: Exception) {
            null
        }
        
        textContent?.let { content ->
            when {
                content.trim().startsWith("{") || content.contains("\"") -> return FileStructure.JSON
                content.trim().startsWith("<") || content.contains("<?xml") -> return FileStructure.XML
                content.contains("=") && content.contains("\n") -> return FileStructure.KEY_VALUE
                isPrintableText(content) -> return FileStructure.TEXT
            }
        }
        
        // Check for binary formats
        when {
            isSQLiteDatabase(bytes) -> return FileStructure.DATABASE
            hasGameFileSignature(bytes) -> return FileStructure.BINARY
            isBinaryData(bytes) -> return FileStructure.BINARY
            else -> return FileStructure.UNKNOWN
        }
    }
    
    /**
     * Analyze binary file structure with advanced pattern recognition
     */
    private fun analyzeBinaryStructure(bytes: ByteArray, file: File): List<PossibleValue> {
        val values = mutableListOf<PossibleValue>()
        val buffer = ByteBuffer.wrap(bytes)
        
        // Check for file signature
        val signature = detectFileSignature(bytes)
        if (signature != null) {
            values.add(
                PossibleValue(
                    key = "file_signature",
                    value = signature.first,
                    dataType = DataType.STRING,
                    confidence = 0.9,
                    description = "File signature: ${signature.second}",
                    location = "File header",
                    category = "metadata"
                )
            )
        }
        
        // Analyze binary data patterns
        values.addAll(analyzeBinaryDataPatterns(buffer, bytes))
        
        // Look for embedded strings
        values.addAll(extractEmbeddedStrings(bytes))
        
        // Analyze data structures
        values.addAll(analyzeDataStructures(bytes))
        
        // Look for repeating patterns
        values.addAll(analyzeRepeatingPatterns(bytes))
        
        return values
    }
    
    /**
     * Analyze binary data patterns with multiple interpretations
     */
    private fun analyzeBinaryDataPatterns(buffer: ByteBuffer, bytes: ByteArray): List<PossibleValue> {
        val values = mutableListOf<PossibleValue>()
        
        // Try different data type interpretations
        binaryPatterns.forEach { (patternName, extractor) ->
            val stepSize = when {
                patternName.contains("int64") || patternName.contains("double") -> 8
                patternName.contains("int32") || patternName.contains("float") -> 4
                else -> 4
            }
            
            for (offset in 0 until bytes.size - stepSize step stepSize) {
                try {
                    val value = extractor(buffer, offset)
                    
                    if (isLikelyGameValue(value)) {
                        val confidence = calculateBinaryValueConfidence(value, offset, bytes, patternName)
                        
                        if (confidence > 0.3) {
                            values.add(
                                PossibleValue(
                                    key = "${patternName}_$offset",
                                    value = value.toString(),
                                    dataType = determineDataTypeFromPattern(patternName),
                                    confidence = confidence,
                                    description = "Binary value ($patternName) at offset $offset",
                                    location = "Offset $offset",
                                    category = "binary_data"
                                )
                            )
                        }
                    }
                } catch (e: Exception) {
                    // Continue with next offset
                }
            }
        }
        
        return values
    }
    
    /**
     * Extract embedded strings from binary data
     */
    private fun extractEmbeddedStrings(bytes: ByteArray): List<PossibleValue> {
        val values = mutableListOf<PossibleValue>()
        val stringBuilder = StringBuilder()
        var stringStart = -1
        
        for (i in bytes.indices) {
            val byte = bytes[i]
            
            if (isPrintableAscii(byte)) {
                if (stringStart == -1) stringStart = i
                stringBuilder.append(byte.toInt().toChar())
            } else {
                if (stringBuilder.length >= 4) {
                    val foundString = stringBuilder.toString()
                    values.addAll(analyzeEmbeddedString(foundString, stringStart))
                }
                stringBuilder.clear()
                stringStart = -1
            }
        }
        
        // Handle string at end of file
        if (stringBuilder.length >= 4) {
            val foundString = stringBuilder.toString()
            values.addAll(analyzeEmbeddedString(foundString, stringStart))
        }
        
        return values
    }
    
    /**
     * Analyze embedded string for game values
     */
    private fun analyzeEmbeddedString(string: String, offset: Int): List<PossibleValue> {
        val values = mutableListOf<PossibleValue>()
        
        // Look for key-value patterns
        val keyValuePatterns = listOf(
            Pattern.compile("(\\w+)[=:](\\d+(?:\\.\\d+)?)"),
            Pattern.compile("\"(\\w+)\"[=:](\\d+(?:\\.\\d+)?)"),
            Pattern.compile("(\\w+)\\s*[=:]\\s*(\\d+(?:\\.\\d+)?)"),
            Pattern.compile("(\\w+)\\s*[=:]\\s*\"(\\d+(?:\\.\\d+)?)\"")
        )
        
        keyValuePatterns.forEach { pattern ->
            val matcher = pattern.matcher(string)
            while (matcher.find()) {
                val key = matcher.group(1)
                val valueStr = matcher.group(2)
                val value = valueStr.toDoubleOrNull()
                
                if (value != null) {
                    val confidence = AdvancedPatternAnalyzer.analyzeKeyValue(key, value, string)?.confidence ?: 0.0
                    
                    if (confidence > 0.4) {
                        values.add(
                            PossibleValue(
                                key = key,
                                value = valueStr,
                                dataType = if (valueStr.contains(".")) DataType.FLOAT else DataType.INTEGER,
                                confidence = confidence,
                                description = "Embedded key-value pair in binary data",
                                location = "Binary offset $offset",
                                category = AdvancedPatternAnalyzer.analyzeKeyValue(key, value, string)?.category ?: "unknown"
                            )
                        )
                    }
                }
            }
        }
        
        // Look for JSON-like structures
        if (string.contains("{") && string.contains("}")) {
            values.addAll(analyzeEmbeddedJson(string, offset))
        }
        
        return values
    }
    
    /**
     * Analyze embedded JSON structures
     */
    private fun analyzeEmbeddedJson(jsonString: String, offset: Int): List<PossibleValue> {
        val values = mutableListOf<PossibleValue>()
        
        try {
            // Extract JSON-like patterns
            val jsonPattern = Pattern.compile("\\{[^{}]*\\}")
            val matcher = jsonPattern.matcher(jsonString)
            
            while (matcher.find()) {
                val jsonLike = matcher.group()
                values.addAll(ValuePatternDetector.analyzeValuePatterns(jsonLike))
            }
            
        } catch (e: Exception) {
            // Not valid JSON, continue
        }
        
        return values
    }
    
    /**
     * Analyze data structures in binary files
     */
    private fun analyzeDataStructures(bytes: ByteArray): List<PossibleValue> {
        val values = mutableListOf<PossibleValue>()
        
        // Look for common game data structure patterns
        val structures = listOf(
            detectPlayerDataStructure(bytes),
            detectInventoryStructure(bytes),
            detectStatsStructure(bytes),
            detectCurrencyStructure(bytes),
            detectProgressStructure(bytes)
        ).flatten()
        
        values.addAll(structures)
        
        return values
    }
    
    /**
     * Detect player data structure patterns
     */
    private fun detectPlayerDataStructure(bytes: ByteArray): List<PossibleValue> {
        val values = mutableListOf<PossibleValue>()
        
        // Look for typical player data patterns:
        // - Level (small integer 1-100)
        // - Experience (larger integer)
        // - Gold/Currency (medium to large integer)
        // - Health/Mana (small to medium integer)
        
        val buffer = ByteBuffer.wrap(bytes)
        
        for (offset in 0 until bytes.size - 16 step 4) {
            try {
                buffer.position(offset)
                
                val possibleLevel = buffer.int
                val possibleExp = buffer.int
                val possibleGold = buffer.int
                val possibleHealth = buffer.int
                
                if (isLikelyPlayerData(possibleLevel, possibleExp, possibleGold, possibleHealth)) {
                    values.add(
                        PossibleValue(
                            key = "player_level",
                            value = possibleLevel.toString(),
                            dataType = DataType.INTEGER,
                            confidence = 0.7,
                            description = "Possible player level in data structure",
                            location = "Offset $offset",
                            category = "progress"
                        )
                    )
                    
                    values.add(
                        PossibleValue(
                            key = "player_experience",
                            value = possibleExp.toString(),
                            dataType = DataType.INTEGER,
                            confidence = 0.6,
                            description = "Possible player experience in data structure",
                            location = "Offset ${offset + 4}",
                            category = "experience"
                        )
                    )
                    
                    values.add(
                        PossibleValue(
                            key = "player_gold",
                            value = possibleGold.toString(),
                            dataType = DataType.INTEGER,
                            confidence = 0.8,
                            description = "Possible player currency in data structure",
                            location = "Offset ${offset + 8}",
                            category = "currency"
                        )
                    )
                    
                    values.add(
                        PossibleValue(
                            key = "player_health",
                            value = possibleHealth.toString(),
                            dataType = DataType.INTEGER,
                            confidence = 0.6,
                            description = "Possible player health in data structure",
                            location = "Offset ${offset + 12}",
                            category = "health_energy"
                        )
                    )
                }
            } catch (e: Exception) {
                // Continue scanning
            }
        }
        
        return values
    }
    
    /**
     * Detect inventory structure patterns
     */
    private fun detectInventoryStructure(bytes: ByteArray): List<PossibleValue> {
        val values = mutableListOf<PossibleValue>()
        
        // Look for repeating patterns that might be inventory items
        val buffer = ByteBuffer.wrap(bytes)
        
        for (offset in 0 until bytes.size - 12 step 4) {
            try {
                buffer.position(offset)
                
                val itemId = buffer.int
                val quantity = buffer.int
                val durability = buffer.int
                
                if (isLikelyInventoryItem(itemId, quantity, durability)) {
                    values.add(
                        PossibleValue(
                            key = "item_quantity",
                            value = quantity.toString(),
                            dataType = DataType.INTEGER,
                            confidence = 0.6,
                            description = "Possible item quantity in inventory structure",
                            location = "Offset ${offset + 4}",
                            category = "inventory"
                        )
                    )
                    
                    if (durability in 1..100) {
                        values.add(
                            PossibleValue(
                                key = "item_durability",
                                value = durability.toString(),
                                dataType = DataType.INTEGER,
                                confidence = 0.5,
                                description = "Possible item durability in inventory structure",
                                location = "Offset ${offset + 8}",
                                category = "inventory"
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                // Continue scanning
            }
        }
        
        return values
    }
    
    /**
     * Detect stats structure patterns
     */
    private fun detectStatsStructure(bytes: ByteArray): List<PossibleValue> {
        val values = mutableListOf<PossibleValue>()
        
        // Look for stat blocks (attack, defense, speed, etc.)
        val buffer = ByteBuffer.wrap(bytes)
        
        for (offset in 0 until bytes.size - 20 step 4) {
            try {
                buffer.position(offset)
                
                val stat1 = buffer.int
                val stat2 = buffer.int
                val stat3 = buffer.int
                val stat4 = buffer.int
                val stat5 = buffer.int
                
                if (isLikelyStatsBlock(stat1, stat2, stat3, stat4, stat5)) {
                    val statNames = listOf("attack", "defense", "speed", "luck", "critical")
                    val statValues = listOf(stat1, stat2, stat3, stat4, stat5)
                    
                    statValues.forEachIndexed { index, statValue ->
                        values.add(
                            PossibleValue(
                                key = "stat_${statNames[index]}",
                                value = statValue.toString(),
                                dataType = DataType.INTEGER,
                                confidence = 0.5,
                                description = "Possible ${statNames[index]} stat in stats structure",
                                location = "Offset ${offset + index * 4}",
                                category = "stats"
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                // Continue scanning
            }
        }
        
        return values
    }
    
    /**
     * Detect currency structure patterns
     */
    private fun detectCurrencyStructure(bytes: ByteArray): List<PossibleValue> {
        val values = mutableListOf<PossibleValue>()
        
        // Look for multiple currency values together
        val buffer = ByteBuffer.wrap(bytes)
        
        for (offset in 0 until bytes.size - 12 step 4) {
            try {
                buffer.position(offset)
                
                val currency1 = buffer.int
                val currency2 = buffer.int
                val currency3 = buffer.int
                
                if (isLikelyCurrencyBlock(currency1, currency2, currency3)) {
                    val currencyNames = listOf("gold", "gems", "tokens")
                    val currencyValues = listOf(currency1, currency2, currency3)
                    
                    currencyValues.forEachIndexed { index, currencyValue ->
                        values.add(
                            PossibleValue(
                                key = "currency_${currencyNames[index]}",
                                value = currencyValue.toString(),
                                dataType = DataType.INTEGER,
                                confidence = 0.7,
                                description = "Possible ${currencyNames[index]} in currency structure",
                                location = "Offset ${offset + index * 4}",
                                category = if (index == 0) "currency" else "premium_currency"
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                // Continue scanning
            }
        }
        
        return values
    }
    
    /**
     * Detect progress structure patterns
     */
    private fun detectProgressStructure(bytes: ByteArray): List<PossibleValue> {
        val values = mutableListOf<PossibleValue>()
        
        // Look for progress-related data
        val buffer = ByteBuffer.wrap(bytes)
        
        for (offset in 0 until bytes.size - 16 step 4) {
            try {
                buffer.position(offset)
                
                val currentLevel = buffer.int
                val currentExp = buffer.int
                val nextLevelExp = buffer.int
                val totalExp = buffer.int
                
                if (isLikelyProgressData(currentLevel, currentExp, nextLevelExp, totalExp)) {
                    values.add(
                        PossibleValue(
                            key = "current_level",
                            value = currentLevel.toString(),
                            dataType = DataType.INTEGER,
                            confidence = 0.8,
                            description = "Current level in progress structure",
                            location = "Offset $offset",
                            category = "progress"
                        )
                    )
                    
                    values.add(
                        PossibleValue(
                            key = "current_experience",
                            value = currentExp.toString(),
                            dataType = DataType.INTEGER,
                            confidence = 0.7,
                            description = "Current experience in progress structure",
                            location = "Offset ${offset + 4}",
                            category = "experience"
                        )
                    )
                    
                    values.add(
                        PossibleValue(
                            key = "total_experience",
                            value = totalExp.toString(),
                            dataType = DataType.INTEGER,
                            confidence = 0.6,
                            description = "Total experience in progress structure",
                            location = "Offset ${offset + 12}",
                            category = "experience"
                        )
                    )
                }
            } catch (e: Exception) {
                // Continue scanning
            }
        }
        
        return values
    }
    
    /**
     * Analyze repeating patterns in binary data
     */
    private fun analyzeRepeatingPatterns(bytes: ByteArray): List<PossibleValue> {
        val values = mutableListOf<PossibleValue>()
        
        // Look for repeating 4-byte and 8-byte patterns
        val patterns = findRepeatingPatterns(bytes, 4) + findRepeatingPatterns(bytes, 8)
        
        patterns.forEach { pattern ->
            if (pattern.occurrences >= 3 && pattern.isLikelyGameData()) {
                values.add(
                    PossibleValue(
                        key = "repeating_pattern_${pattern.size}",
                        value = pattern.value.toString(),
                        dataType = DataType.INTEGER,
                        confidence = 0.4,
                        description = "Repeating ${pattern.size}-byte pattern (${pattern.occurrences} occurrences)",
                        location = "Multiple offsets",
                        category = "patterns"
                    )
                )
            }
        }
        
        return values
    }
    
    /**
     * Find repeating patterns in binary data
     */
    private fun findRepeatingPatterns(bytes: ByteArray, patternSize: Int): List<RepeatingPattern> {
        val patterns = mutableMapOf<String, RepeatingPattern>()
        
        for (offset in 0 until bytes.size - patternSize step patternSize) {
            val pattern = bytes.sliceArray(offset until offset + patternSize)
            val patternKey = pattern.joinToString("") { "%02x".format(it) }
            
            val existingPattern = patterns[patternKey]
            if (existingPattern != null) {
                existingPattern.occurrences++
                existingPattern.offsets.add(offset)
            } else {
                val value = when (patternSize) {
                    4 -> ByteBuffer.wrap(pattern).int.toLong()
                    8 -> ByteBuffer.wrap(pattern).long
                    else -> 0L
                }
                
                patterns[patternKey] = RepeatingPattern(
                    pattern = pattern,
                    size = patternSize,
                    value = value,
                    occurrences = 1,
                    offsets = mutableListOf(offset)
                )
            }
        }
        
        return patterns.values.filter { it.occurrences >= 2 }
    }
    
    /**
     * Data class for repeating patterns
     */
    data class RepeatingPattern(
        val pattern: ByteArray,
        val size: Int,
        val value: Long,
        var occurrences: Int,
        val offsets: MutableList<Int>
    ) {
        fun isLikelyGameData(): Boolean {
            return value in 1..1000000 && value != 0L
        }
    }
    
    // Helper functions for structure detection
    
    private fun isLikelyPlayerData(level: Int, exp: Int, gold: Int, health: Int): Boolean {
        return level in 1..1000 &&
               exp >= level * 100 &&
               gold >= 0 &&
               health in 1..10000 &&
               exp > level // Experience should be higher than level
    }
    
    private fun isLikelyInventoryItem(itemId: Int, quantity: Int, durability: Int): Boolean {
        return itemId > 0 &&
               quantity in 1..9999 &&
               (durability in 0..100 || durability == -1) // -1 might indicate infinite durability
    }
    
    private fun isLikelyStatsBlock(vararg stats: Int): Boolean {
        return stats.all { it in 1..1000 } &&
               stats.distinct().size >= 3 // At least 3 different values
    }
    
    private fun isLikelyCurrencyBlock(currency1: Int, currency2: Int, currency3: Int): Boolean {
        return currency1 >= 0 && currency2 >= 0 && currency3 >= 0 &&
               (currency1 > 0 || currency2 > 0 || currency3 > 0) // At least one non-zero
    }
    
    private fun isLikelyProgressData(currentLevel: Int, currentExp: Int, nextLevelExp: Int, totalExp: Int): Boolean {
        return currentLevel in 1..1000 &&
               currentExp >= 0 &&
               nextLevelExp > currentExp &&
               totalExp >= currentExp &&
               nextLevelExp <= totalExp * 2 // Reasonable relationship
    }
    
    private fun detectFileSignature(bytes: ByteArray): Pair<String, String>? {
        if (bytes.size < 4) return null
        
        val header = String(bytes.take(4).toByteArray(), Charsets.UTF_8)
        return gameFileSignatures[header]?.let { header to it }
    }
    
    private fun hasGameFileSignature(bytes: ByteArray): Boolean {
        return detectFileSignature(bytes) != null
    }
    
    private fun isSQLiteDatabase(bytes: ByteArray): Boolean {
        if (bytes.size < 16) return false
        val header = String(bytes.take(6).toByteArray(), Charsets.UTF_8)
        return header == "SQLite"
    }
    
    private fun isBinaryData(bytes: ByteArray): Boolean {
        val nonPrintableCount = bytes.take(1024).count { !isPrintableAscii(it) }
        return nonPrintableCount > bytes.size * 0.3
    }
    
    private fun isPrintableAscii(byte: Byte): Boolean {
        val unsigned = byte.toInt() and 0xFF
        return unsigned in 32..126 || unsigned in listOf(9, 10, 13) // Printable + tab, newline, carriage return
    }
    
    private fun isPrintableText(text: String): Boolean {
        val printableCount = text.count { it.code in 32..126 || it in listOf('\t', '\n', '\r') }
        return printableCount > text.length * 0.8
    }
    
    private fun isLikelyGameValue(value: Any): Boolean {
        return when (value) {
            is Int -> value in 1..100000000 && value != 0
            is Long -> value in 1L..100000000L && value != 0L
            is Float -> value > 0.0f && value <= 1000000.0f && !value.isNaN() && !value.isInfinite()
            is Double -> value > 0.0 && value <= 1000000.0 && !value.isNaN() && !value.isInfinite()
            else -> false
        }
    }
    
    private fun calculateBinaryValueConfidence(
        value: Any, 
        offset: Int, 
        bytes: ByteArray, 
        patternName: String
    ): Double {
        var confidence = 0.3 // Base confidence
        
        // Value-based confidence
        when (value) {
            is Int -> {
                when {
                    value in 1..100 -> confidence += 0.3
                    value in 100..10000 -> confidence += 0.4
                    value in 10000..1000000 -> confidence += 0.3
                    value == 0 -> confidence -= 0.2
                    value < 0 -> confidence -= 0.3
                }
            }
            is Long -> {
                when {
                    value in 1L..100L -> confidence += 0.3
                    value in 100L..10000L -> confidence += 0.4
                    value in 10000L..1000000L -> confidence += 0.3
                    value == 0L -> confidence -= 0.2
                    value < 0L -> confidence -= 0.3
                }
            }
            is Float -> {
                when {
                    value in 0.0f..1.0f -> confidence += 0.4 // Likely percentage/ratio
                    value in 1.0f..1000.0f -> confidence += 0.3
                    value.isNaN() || value.isInfinite() -> confidence -= 0.5
                }
            }
        }
        
        // Position-based confidence
        if (offset % 4 == 0) confidence += 0.1 // Aligned to 4-byte boundary
        if (offset % 8 == 0) confidence += 0.1 // Aligned to 8-byte boundary
        
        // Pattern-based confidence
        when {
            patternName.contains("le") -> confidence += 0.05 // Little endian more common
            patternName.contains("int32") -> confidence += 0.1 // 32-bit integers common in games
        }
        
        return confidence.coerceIn(0.0, 1.0)
    }
    
    private fun determineDataTypeFromPattern(patternName: String): DataType {
        return when {
            patternName.contains("int") -> DataType.INTEGER
            patternName.contains("float") || patternName.contains("double") -> DataType.FLOAT
            else -> DataType.INTEGER
        }
    }
    
    // Placeholder implementations for other structure types
    private fun analyzeJsonStructure(file: File): List<PossibleValue> {
        return DeepFileAnalyzer.performDeepAnalysis(
            com.gamefileinspector.models.GameFile(
                path = file.absolutePath,
                name = file.name,
                size = file.length(),
                lastModified = file.lastModified(),
                gamePackage = "unknown",
                isReadable = file.canRead(),
                isWritable = file.canWrite()
            )
        )
    }
    
    private fun analyzeXmlStructure(file: File): List<PossibleValue> {
        return DeepFileAnalyzer.performDeepAnalysis(
            com.gamefileinspector.models.GameFile(
                path = file.absolutePath,
                name = file.name,
                size = file.length(),
                lastModified = file.lastModified(),
                gamePackage = "unknown",
                isReadable = file.canRead(),
                isWritable = file.canWrite()
            )
        )
    }
    
    private fun analyzeKeyValueStructure(file: File): List<PossibleValue> {
        return DeepFileAnalyzer.performDeepAnalysis(
            com.gamefileinspector.models.GameFile(
                path = file.absolutePath,
                name = file.name,
                size = file.length(),
                lastModified = file.lastModified(),
                gamePackage = "unknown",
                isReadable = file.canRead(),
                isWritable = file.canWrite()
            )
        )
    }
    
    private fun analyzeDatabaseStructure(bytes: ByteArray, file: File): List<PossibleValue> {
        // For now, treat as binary
        return analyzeBinaryStructure(bytes, file)
    }
    
    private fun analyzeTextStructure(file: File): List<PossibleValue> {
        return ValuePatternDetector.analyzeValuePatterns(file.readText())
    }
    
    private fun analyzeUnknownStructure(bytes: ByteArray, file: File): List<PossibleValue> {
        // Try both binary and text analysis
        val binaryValues = analyzeBinaryStructure(bytes, file)
        val textValues = try {
            analyzeTextStructure(file)
        } catch (e: Exception) {
            emptyList()
        }
        
        return (binaryValues + textValues).distinctBy { "${it.key}_${it.value}" }
    }
    
    private fun analyzeBasicStructure(file: File): List<PossibleValue> {
        return try {
            val content = file.readText()
            ValuePatternDetector.analyzeValuePatterns(content)
        } catch (e: Exception) {
            emptyList()
        }
    }
}