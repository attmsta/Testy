package com.gamefileinspector.analyzers

import com.gamefileinspector.models.DataType
import com.gamefileinspector.models.PossibleValue
import java.util.regex.Pattern
import kotlin.math.*

/**
 * Advanced value pattern detector for identifying game values through statistical analysis
 */
object ValuePatternDetector {
    
    // Statistical patterns that indicate game values
    data class ValuePattern(
        val name: String,
        val description: String,
        val detector: (List<Number>) -> Double,
        val category: String
    )
    
    // Common game value patterns
    private val gameValuePatterns = listOf(
        ValuePattern(
            "currency_progression",
            "Values that increase in typical currency patterns",
            ::detectCurrencyProgression,
            "currency"
        ),
        ValuePattern(
            "level_sequence",
            "Sequential level progression (1, 2, 3, etc.)",
            ::detectLevelSequence,
            "progress"
        ),
        ValuePattern(
            "experience_curve",
            "Exponential experience point progression",
            ::detectExperienceCurve,
            "experience"
        ),
        ValuePattern(
            "percentage_values",
            "Values that represent percentages (0-100)",
            ::detectPercentageValues,
            "stats"
        ),
        ValuePattern(
            "round_numbers",
            "Round numbers often used in games",
            ::detectRoundNumbers,
            "currency"
        ),
        ValuePattern(
            "fibonacci_like",
            "Fibonacci-like sequences common in game progression",
            ::detectFibonacciLike,
            "progress"
        ),
        ValuePattern(
            "power_of_two",
            "Powers of 2 often used in game mechanics",
            ::detectPowerOfTwo,
            "stats"
        ),
        ValuePattern(
            "health_mana_pattern",
            "Typical health/mana value patterns",
            ::detectHealthManaPattern,
            "health_energy"
        ),
        ValuePattern(
            "achievement_scores",
            "High scores typical of achievements",
            ::detectAchievementScores,
            "achievements"
        ),
        ValuePattern(
            "inventory_quantities",
            "Small integer quantities typical of inventory",
            ::detectInventoryQuantities,
            "inventory"
        )
    )
    
    // Advanced regex patterns for complex value detection
    private val advancedPatterns = mapOf(
        "nested_currency" to Pattern.compile("(?:gold|coin|money|cash|credit).*?(\\d+).*?(?:gold|coin|money|cash|credit).*?(\\d+)", Pattern.CASE_INSENSITIVE),
        "stat_block" to Pattern.compile("(?:attack|damage|defense|armor|speed|agility).*?(\\d+)", Pattern.CASE_INSENSITIVE),
        "coordinate_pair" to Pattern.compile("(?:x|y|pos|position|coord).*?(\\d+).*?(?:x|y|pos|position|coord).*?(\\d+)", Pattern.CASE_INSENSITIVE),
        "time_duration" to Pattern.compile("(?:time|duration|cooldown|timer).*?(\\d+)", Pattern.CASE_INSENSITIVE),
        "resource_count" to Pattern.compile("(?:wood|stone|iron|food|oil|energy).*?(\\d+)", Pattern.CASE_INSENSITIVE),
        "building_level" to Pattern.compile("(?:building|structure|tower|wall).*?(?:level|lvl).*?(\\d+)", Pattern.CASE_INSENSITIVE),
        "skill_level" to Pattern.compile("(?:skill|ability|talent|mastery).*?(?:level|lvl).*?(\\d+)", Pattern.CASE_INSENSITIVE),
        "quest_progress" to Pattern.compile("(?:quest|mission|task).*?(?:progress|complete|done).*?(\\d+)", Pattern.CASE_INSENSITIVE)
    )
    
    // Value relationship patterns
    private val relationshipPatterns = listOf(
        "sum_relationship" to { values: List<Number> -> detectSumRelationship(values) },
        "ratio_relationship" to { values: List<Number> -> detectRatioRelationship(values) },
        "difference_relationship" to { values: List<Number> -> detectDifferenceRelationship(values) },
        "multiplication_relationship" to { values: List<Number> -> detectMultiplicationRelationship(values) }
    )
    
    /**
     * Analyze content for advanced value patterns
     */
    fun analyzeValuePatterns(content: String): List<PossibleValue> {
        val detectedValues = mutableListOf<PossibleValue>()
        
        // Extract all numeric values with context
        val numericValues = extractNumericValuesWithContext(content)
        
        // Apply pattern detection
        detectedValues.addAll(detectAdvancedPatterns(content))
        detectedValues.addAll(detectStatisticalPatterns(numericValues))
        detectedValues.addAll(detectRelationshipPatterns(numericValues))
        detectedValues.addAll(detectContextualPatterns(content))
        
        return detectedValues.distinctBy { "${it.key}_${it.value}_${it.location}" }
            .sortedByDescending { it.confidence }
    }
    
    /**
     * Extract numeric values with their surrounding context
     */
    private fun extractNumericValuesWithContext(content: String): List<NumericValueContext> {
        val values = mutableListOf<NumericValueContext>()
        val lines = content.split('\n')
        
        lines.forEachIndexed { lineIndex, line ->
            val numberPattern = Pattern.compile("(\\w*\\W*)(\\d+(?:\\.\\d+)?)(\\W*\\w*)")
            val matcher = numberPattern.matcher(line)
            
            while (matcher.find()) {
                val beforeContext = matcher.group(1) ?: ""
                val numberStr = matcher.group(2)
                val afterContext = matcher.group(3) ?: ""
                val number = numberStr.toDoubleOrNull()
                
                if (number != null) {
                    values.add(
                        NumericValueContext(
                            value = number,
                            beforeContext = beforeContext.trim(),
                            afterContext = afterContext.trim(),
                            lineNumber = lineIndex + 1,
                            fullLine = line,
                            position = matcher.start(2)
                        )
                    )
                }
            }
        }
        
        return values
    }
    
    /**
     * Data class for numeric values with context
     */
    data class NumericValueContext(
        val value: Double,
        val beforeContext: String,
        val afterContext: String,
        val lineNumber: Int,
        val fullLine: String,
        val position: Int
    )
    
    /**
     * Detect advanced regex patterns
     */
    private fun detectAdvancedPatterns(content: String): List<PossibleValue> {
        val values = mutableListOf<PossibleValue>()
        
        advancedPatterns.forEach { (patternName, pattern) ->
            val matcher = pattern.matcher(content)
            
            while (matcher.find()) {
                for (i in 1..matcher.groupCount()) {
                    val value = matcher.group(i)
                    val numValue = value.toDoubleOrNull()
                    
                    if (numValue != null) {
                        val confidence = calculatePatternConfidence(patternName, numValue, matcher.group(0))
                        
                        if (confidence > 0.4) {
                            values.add(
                                PossibleValue(
                                    key = "${patternName}_${i}",
                                    value = value,
                                    dataType = if (value.contains(".")) DataType.FLOAT else DataType.INTEGER,
                                    confidence = confidence,
                                    description = "Advanced pattern: ${patternName.replace("_", " ")}",
                                    location = "Pattern match at position ${matcher.start()}",
                                    category = determinePatternCategory(patternName)
                                )
                            )
                        }
                    }
                }
            }
        }
        
        return values
    }
    
    /**
     * Detect statistical patterns in numeric values
     */
    private fun detectStatisticalPatterns(values: List<NumericValueContext>): List<PossibleValue> {
        val detectedValues = mutableListOf<PossibleValue>()
        
        if (values.size < 3) return detectedValues
        
        val numbers = values.map { it.value }
        
        gameValuePatterns.forEach { pattern ->
            val confidence = pattern.detector(numbers)
            
            if (confidence > 0.5) {
                // Find the most representative value for this pattern
                val representativeValue = findRepresentativeValue(numbers, pattern)
                val context = values.find { it.value == representativeValue }
                
                if (context != null) {
                    detectedValues.add(
                        PossibleValue(
                            key = pattern.name,
                            value = representativeValue.toString(),
                            dataType = if (representativeValue % 1.0 == 0.0) DataType.INTEGER else DataType.FLOAT,
                            confidence = confidence,
                            description = pattern.description,
                            location = "Line ${context.lineNumber} (statistical pattern)",
                            category = pattern.category
                        )
                    )
                }
            }
        }
        
        return detectedValues
    }
    
    /**
     * Detect relationship patterns between values
     */
    private fun detectRelationshipPatterns(values: List<NumericValueContext>): List<PossibleValue> {
        val detectedValues = mutableListOf<PossibleValue>()
        
        if (values.size < 2) return detectedValues
        
        val numbers = values.map { it.value }
        
        relationshipPatterns.forEach { (relationshipName, detector) ->
            val confidence = detector(numbers)
            
            if (confidence > 0.6) {
                // Create a composite value representing the relationship
                val relatedValues = numbers.take(3).joinToString(", ")
                
                detectedValues.add(
                    PossibleValue(
                        key = relationshipName,
                        value = relatedValues,
                        dataType = DataType.STRING,
                        confidence = confidence,
                        description = "Value relationship: ${relationshipName.replace("_", " ")}",
                        location = "Multiple values (relationship pattern)",
                        category = "relationships"
                    )
                )
            }
        }
        
        return detectedValues
    }
    
    /**
     * Detect contextual patterns based on surrounding text
     */
    private fun detectContextualPatterns(content: String): List<PossibleValue> {
        val values = mutableListOf<PossibleValue>()
        
        // Look for values near game-related keywords
        val gameKeywords = mapOf(
            "player" to "progress",
            "character" to "progress", 
            "level" to "progress",
            "score" to "achievements",
            "gold" to "currency",
            "coin" to "currency",
            "gem" to "premium_currency",
            "diamond" to "premium_currency",
            "health" to "health_energy",
            "mana" to "health_energy",
            "energy" to "health_energy",
            "attack" to "stats",
            "defense" to "stats",
            "speed" to "stats",
            "inventory" to "inventory",
            "item" to "inventory",
            "weapon" to "inventory",
            "armor" to "inventory"
        )
        
        gameKeywords.forEach { (keyword, category) ->
            val pattern = Pattern.compile("\\b$keyword\\b.{0,50}?(\\d+(?:\\.\\d+)?)", Pattern.CASE_INSENSITIVE)
            val matcher = pattern.matcher(content)
            
            while (matcher.find()) {
                val value = matcher.group(1)
                val numValue = value.toDoubleOrNull()
                
                if (numValue != null) {
                    val confidence = calculateContextualConfidence(keyword, numValue, matcher.group(0))
                    
                    if (confidence > 0.5) {
                        values.add(
                            PossibleValue(
                                key = "${keyword}_value",
                                value = value,
                                dataType = if (value.contains(".")) DataType.FLOAT else DataType.INTEGER,
                                confidence = confidence,
                                description = "Value near '$keyword' keyword",
                                location = "Near keyword '$keyword'",
                                category = category
                            )
                        )
                    }
                }
            }
        }
        
        return values
    }
    
    // Statistical pattern detectors
    
    private fun detectCurrencyProgression(values: List<Number>): Double {
        if (values.size < 3) return 0.0
        
        val sortedValues = values.map { it.toDouble() }.sorted()
        var progressionScore = 0.0
        
        // Check for typical currency patterns (multiples of 10, 100, 1000)
        val roundNumberCount = sortedValues.count { value ->
            value % 10 == 0.0 || value % 100 == 0.0 || value % 1000 == 0.0
        }
        progressionScore += (roundNumberCount.toDouble() / values.size) * 0.4
        
        // Check for increasing progression
        val isIncreasing = sortedValues.zipWithNext().all { (a, b) -> b >= a }
        if (isIncreasing) progressionScore += 0.3
        
        // Check for reasonable currency ranges
        val inCurrencyRange = sortedValues.count { it in 1.0..10000000.0 }
        progressionScore += (inCurrencyRange.toDouble() / values.size) * 0.3
        
        return progressionScore.coerceIn(0.0, 1.0)
    }
    
    private fun detectLevelSequence(values: List<Number>): Double {
        if (values.size < 3) return 0.0
        
        val intValues = values.map { it.toInt() }.sorted()
        
        // Check for sequential progression
        val isSequential = intValues.zipWithNext().all { (a, b) -> b == a + 1 }
        if (isSequential) return 0.9
        
        // Check for mostly sequential with some gaps
        val gaps = intValues.zipWithNext().map { (a, b) -> b - a }
        val smallGaps = gaps.count { it in 1..3 }
        val sequentialScore = smallGaps.toDouble() / gaps.size
        
        // Check for reasonable level ranges
        val inLevelRange = intValues.count { it in 1..1000 }
        val rangeScore = inLevelRange.toDouble() / values.size
        
        return (sequentialScore * 0.6 + rangeScore * 0.4).coerceIn(0.0, 1.0)
    }
    
    private fun detectExperienceCurve(values: List<Number>): Double {
        if (values.size < 4) return 0.0
        
        val sortedValues = values.map { it.toDouble() }.sorted()
        
        // Check for exponential-like growth
        val ratios = sortedValues.zipWithNext().map { (a, b) -> 
            if (a > 0) b / a else 0.0 
        }.filter { it > 0 }
        
        if (ratios.isEmpty()) return 0.0
        
        // Experience typically grows exponentially
        val avgRatio = ratios.average()
        val exponentialScore = when {
            avgRatio in 1.1..3.0 -> 0.8 // Good exponential growth
            avgRatio in 1.0..1.1 -> 0.4 // Linear growth
            avgRatio > 3.0 -> 0.6 // High exponential growth
            else -> 0.2
        }
        
        // Check for reasonable experience ranges
        val inExpRange = sortedValues.count { it in 100.0..10000000.0 }
        val rangeScore = inExpRange.toDouble() / values.size
        
        return (exponentialScore * 0.7 + rangeScore * 0.3).coerceIn(0.0, 1.0)
    }
    
    private fun detectPercentageValues(values: List<Number>): Double {
        val percentageCount = values.count { it.toDouble() in 0.0..100.0 }
        return (percentageCount.toDouble() / values.size).coerceIn(0.0, 1.0)
    }
    
    private fun detectRoundNumbers(values: List<Number>): Double {
        val roundCount = values.count { value ->
            val num = value.toDouble()
            num % 10 == 0.0 || num % 100 == 0.0 || num % 1000 == 0.0 || num % 5 == 0.0
        }
        return (roundCount.toDouble() / values.size).coerceIn(0.0, 1.0)
    }
    
    private fun detectFibonacciLike(values: List<Number>): Double {
        if (values.size < 3) return 0.0
        
        val intValues = values.map { it.toInt() }.sorted()
        var fibScore = 0.0
        
        for (i in 2 until intValues.size) {
            val sum = intValues[i-2] + intValues[i-1]
            if (abs(intValues[i] - sum) <= 1) {
                fibScore += 1.0
            }
        }
        
        return (fibScore / (intValues.size - 2)).coerceIn(0.0, 1.0)
    }
    
    private fun detectPowerOfTwo(values: List<Number>): Double {
        val powerOfTwoCount = values.count { value ->
            val num = value.toInt()
            num > 0 && (num and (num - 1)) == 0
        }
        return (powerOfTwoCount.toDouble() / values.size).coerceIn(0.0, 1.0)
    }
    
    private fun detectHealthManaPattern(values: List<Number>): Double {
        val healthManaValues = values.filter { it.toDouble() in 1.0..1000.0 }
        val roundValues = healthManaValues.count { it.toDouble() % 5 == 0.0 || it.toDouble() % 10 == 0.0 }
        
        if (healthManaValues.isEmpty()) return 0.0
        
        return (roundValues.toDouble() / healthManaValues.size).coerceIn(0.0, 1.0)
    }
    
    private fun detectAchievementScores(values: List<Number>): Double {
        val highScores = values.count { it.toDouble() > 10000 }
        val roundScores = values.count { it.toDouble() % 100 == 0.0 || it.toDouble() % 1000 == 0.0 }
        
        val highScoreRatio = highScores.toDouble() / values.size
        val roundScoreRatio = roundScores.toDouble() / values.size
        
        return (highScoreRatio * 0.6 + roundScoreRatio * 0.4).coerceIn(0.0, 1.0)
    }
    
    private fun detectInventoryQuantities(values: List<Number>): Double {
        val smallIntegers = values.count { 
            val num = it.toDouble()
            num == num.toInt().toDouble() && num in 1.0..999.0
        }
        return (smallIntegers.toDouble() / values.size).coerceIn(0.0, 1.0)
    }
    
    // Relationship pattern detectors
    
    private fun detectSumRelationship(values: List<Number>): Double {
        if (values.size < 3) return 0.0
        
        val nums = values.map { it.toDouble() }
        var sumMatches = 0
        
        for (i in 2 until nums.size) {
            if (abs(nums[i] - (nums[i-1] + nums[i-2])) < 0.01) {
                sumMatches++
            }
        }
        
        return (sumMatches.toDouble() / (nums.size - 2)).coerceIn(0.0, 1.0)
    }
    
    private fun detectRatioRelationship(values: List<Number>): Double {
        if (values.size < 3) return 0.0
        
        val nums = values.map { it.toDouble() }.filter { it > 0 }
        if (nums.size < 3) return 0.0
        
        val ratios = nums.zipWithNext().map { (a, b) -> b / a }
        val avgRatio = ratios.average()
        
        val consistentRatios = ratios.count { abs(it - avgRatio) < avgRatio * 0.1 }
        return (consistentRatios.toDouble() / ratios.size).coerceIn(0.0, 1.0)
    }
    
    private fun detectDifferenceRelationship(values: List<Number>): Double {
        if (values.size < 3) return 0.0
        
        val nums = values.map { it.toDouble() }
        val differences = nums.zipWithNext().map { (a, b) -> b - a }
        val avgDiff = differences.average()
        
        val consistentDiffs = differences.count { abs(it - avgDiff) < abs(avgDiff) * 0.1 }
        return (consistentDiffs.toDouble() / differences.size).coerceIn(0.0, 1.0)
    }
    
    private fun detectMultiplicationRelationship(values: List<Number>): Double {
        if (values.size < 3) return 0.0
        
        val nums = values.map { it.toDouble() }
        var multiplicationMatches = 0
        
        for (i in 2 until nums.size) {
            val product = nums[i-1] * nums[i-2]
            if (abs(nums[i] - product) < product * 0.01) {
                multiplicationMatches++
            }
        }
        
        return (multiplicationMatches.toDouble() / (nums.size - 2)).coerceIn(0.0, 1.0)
    }
    
    // Helper functions
    
    private fun calculatePatternConfidence(patternName: String, value: Double, context: String): Double {
        var confidence = 0.5 // Base confidence for pattern match
        
        // Adjust based on pattern type
        when (patternName) {
            "nested_currency" -> confidence += 0.3
            "stat_block" -> confidence += 0.25
            "coordinate_pair" -> confidence += 0.2
            "time_duration" -> confidence += 0.15
            "resource_count" -> confidence += 0.25
            "building_level" -> confidence += 0.2
            "skill_level" -> confidence += 0.2
            "quest_progress" -> confidence += 0.15
        }
        
        // Adjust based on value characteristics
        when {
            value in 1.0..100.0 -> confidence += 0.1
            value in 100.0..10000.0 -> confidence += 0.15
            value > 10000.0 -> confidence += 0.1
            value == 0.0 -> confidence -= 0.2
        }
        
        return confidence.coerceIn(0.0, 1.0)
    }
    
    private fun calculateContextualConfidence(keyword: String, value: Double, context: String): Double {
        var confidence = 0.6 // Base confidence for contextual match
        
        // Keyword-specific adjustments
        when (keyword.lowercase()) {
            "gold", "coin", "money" -> {
                if (value in 100.0..1000000.0) confidence += 0.2
                if (value % 10 == 0.0) confidence += 0.1
            }
            "level", "lvl" -> {
                if (value in 1.0..100.0) confidence += 0.3
                if (value == value.toInt().toDouble()) confidence += 0.1
            }
            "health", "hp" -> {
                if (value in 1.0..1000.0) confidence += 0.2
                if (value % 5 == 0.0) confidence += 0.1
            }
            "experience", "exp", "xp" -> {
                if (value > 100.0) confidence += 0.2
            }
        }
        
        return confidence.coerceIn(0.0, 1.0)
    }
    
    private fun determinePatternCategory(patternName: String): String {
        return when (patternName) {
            "nested_currency", "resource_count" -> "currency"
            "stat_block" -> "stats"
            "coordinate_pair" -> "position"
            "time_duration" -> "time"
            "building_level", "skill_level" -> "progress"
            "quest_progress" -> "achievements"
            else -> "unknown"
        }
    }
    
    private fun findRepresentativeValue(values: List<Number>, pattern: ValuePattern): Double {
        // Return the median value as most representative
        val sorted = values.map { it.toDouble() }.sorted()
        return if (sorted.size % 2 == 0) {
            (sorted[sorted.size / 2 - 1] + sorted[sorted.size / 2]) / 2.0
        } else {
            sorted[sorted.size / 2]
        }
    }
}