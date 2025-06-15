package com.gamefileinspector

import com.gamefileinspector.analyzers.AdvancedPatternAnalyzer
import com.gamefileinspector.models.DataType
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class AdvancedPatternAnalyzerTest {

    @Test
    fun testAnalyzeCurrencyValues() {
        // Test gold detection
        val goldValue = AdvancedPatternAnalyzer.analyzeKeyValue("player_gold", 12500)
        assertNotNull("Should detect gold value", goldValue)
        assertEquals("Should categorize as gold", "gold", goldValue?.category)
        assertTrue("Should have high confidence", goldValue?.confidence ?: 0.0 > 0.7)
        
        // Test gems detection
        val gemsValue = AdvancedPatternAnalyzer.analyzeKeyValue("premium_gems", 150)
        assertNotNull("Should detect gems value", gemsValue)
        assertEquals("Should categorize as gems", "gems", gemsValue?.category)
        
        // Test coins detection
        val coinsValue = AdvancedPatternAnalyzer.analyzeKeyValue("total_coins", 5000)
        assertNotNull("Should detect coins value", coinsValue)
        assertEquals("Should categorize as gold", "gold", coinsValue?.category)
    }

    @Test
    fun testAnalyzeProgressValues() {
        // Test level detection
        val levelValue = AdvancedPatternAnalyzer.analyzeKeyValue("player_level", 25)
        assertNotNull("Should detect level value", levelValue)
        assertEquals("Should categorize as progress", "progress", levelValue?.category)
        assertTrue("Should have good confidence", levelValue?.confidence ?: 0.0 > 0.6)
        
        // Test stage detection
        val stageValue = AdvancedPatternAnalyzer.analyzeKeyValue("current_stage", 15)
        assertNotNull("Should detect stage value", stageValue)
        assertEquals("Should categorize as progress", "progress", stageValue?.category)
        
        // Test rank detection
        val rankValue = AdvancedPatternAnalyzer.analyzeKeyValue("player_rank", 8)
        assertNotNull("Should detect rank value", rankValue)
        assertEquals("Should categorize as progress", "progress", rankValue?.category)
    }

    @Test
    fun testAnalyzeExperienceValues() {
        // Test experience detection
        val expValue = AdvancedPatternAnalyzer.analyzeKeyValue("total_experience", 15750)
        assertNotNull("Should detect experience value", expValue)
        assertEquals("Should categorize as experience", "experience", expValue?.category)
        
        // Test XP detection
        val xpValue = AdvancedPatternAnalyzer.analyzeKeyValue("player_xp", 8500)
        assertNotNull("Should detect XP value", xpValue)
        assertEquals("Should categorize as experience", "experience", xpValue?.category)
        
        // Test skill points detection
        val spValue = AdvancedPatternAnalyzer.analyzeKeyValue("skill_points", 120)
        assertNotNull("Should detect skill points value", spValue)
        assertEquals("Should categorize as experience", "experience", spValue?.category)
    }

    @Test
    fun testAnalyzeScoreValues() {
        // Test score detection
        val scoreValue = AdvancedPatternAnalyzer.analyzeKeyValue("high_score", 98750)
        assertNotNull("Should detect score value", scoreValue)
        assertEquals("Should categorize as score", "score", scoreValue?.category)
        
        // Test points detection
        val pointsValue = AdvancedPatternAnalyzer.analyzeKeyValue("total_points", 45000)
        assertNotNull("Should detect points value", pointsValue)
        assertEquals("Should categorize as score", "score", pointsValue?.category)
    }

    @Test
    fun testAnalyzeCombatValues() {
        // Test health detection
        val healthValue = AdvancedPatternAnalyzer.analyzeKeyValue("player_health", 100)
        assertNotNull("Should detect health value", healthValue)
        assertEquals("Should categorize as combat", "combat", healthValue?.category)
        
        // Test damage detection
        val damageValue = AdvancedPatternAnalyzer.analyzeKeyValue("weapon_damage", 45)
        assertNotNull("Should detect damage value", damageValue)
        assertEquals("Should categorize as combat", "combat", damageValue?.category)
        
        // Test armor detection
        val armorValue = AdvancedPatternAnalyzer.analyzeKeyValue("player_armor", 25)
        assertNotNull("Should detect armor value", armorValue)
        assertEquals("Should categorize as combat", "combat", armorValue?.category)
    }

    @Test
    fun testAnalyzeDataTypes() {
        // Test integer detection
        val intValue = AdvancedPatternAnalyzer.analyzeKeyValue("test_int", 123)
        assertEquals("Should detect integer type", DataType.INTEGER, intValue?.dataType)
        
        // Test float detection
        val floatValue = AdvancedPatternAnalyzer.analyzeKeyValue("test_float", 12.34)
        assertEquals("Should detect float type", DataType.FLOAT, floatValue?.dataType)
        
        // Test boolean detection
        val boolValue = AdvancedPatternAnalyzer.analyzeKeyValue("test_bool", true)
        assertEquals("Should detect boolean type", DataType.BOOLEAN, boolValue?.dataType)
        
        // Test string detection
        val stringValue = AdvancedPatternAnalyzer.analyzeKeyValue("test_string", "hello")
        assertEquals("Should detect string type", DataType.STRING, stringValue?.dataType)
    }

    @Test
    fun testConfidenceScoring() {
        // High confidence values
        val goldValue = AdvancedPatternAnalyzer.analyzeKeyValue("player_gold", 10000)
        assertTrue("Gold should have high confidence", goldValue?.confidence ?: 0.0 > 0.7)
        
        val levelValue = AdvancedPatternAnalyzer.analyzeKeyValue("player_level", 25)
        assertTrue("Level should have good confidence", levelValue?.confidence ?: 0.0 > 0.6)
        
        // Lower confidence values
        val unknownValue = AdvancedPatternAnalyzer.analyzeKeyValue("random_value", 42)
        assertTrue("Unknown value should have lower confidence", unknownValue?.confidence ?: 1.0 < 0.5)
        
        // Zero values should have reduced confidence
        val zeroValue = AdvancedPatternAnalyzer.analyzeKeyValue("player_gold", 0)
        if (zeroValue != null) {
            assertTrue("Zero values should have reduced confidence", zeroValue.confidence < 0.8)
        }
    }

    @Test
    fun testValueRangeConfidence() {
        // Small values (likely levels)
        val smallValue = AdvancedPatternAnalyzer.analyzeKeyValue("player_level", 15)
        assertNotNull("Should detect small value", smallValue)
        
        // Medium values (likely currency)
        val mediumValue = AdvancedPatternAnalyzer.analyzeKeyValue("player_gold", 5000)
        assertNotNull("Should detect medium value", mediumValue)
        
        // Large values (possibly scores)
        val largeValue = AdvancedPatternAnalyzer.analyzeKeyValue("high_score", 500000)
        assertNotNull("Should detect large value", largeValue)
        
        // Very large values (less likely to be game values)
        val veryLargeValue = AdvancedPatternAnalyzer.analyzeKeyValue("timestamp", 1640995200000L)
        // This might not be detected as a game value due to size
        if (veryLargeValue != null) {
            assertTrue("Very large values should have low confidence", veryLargeValue.confidence < 0.5)
        }
    }

    @Test
    fun testAnalyzeBinaryData() {
        // Create test binary data with some integer values
        val binaryData = byteArrayOf(
            0x00, 0x00, 0x13, 0x88.toByte(), // 5000 as 32-bit big-endian int
            0x00, 0x00, 0x00, 0x0A, // 10 as 32-bit big-endian int
            0x00, 0x00, 0x09, 0xC4.toByte(), // 2500 as 32-bit big-endian int
            0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte() // -1 (should be ignored)
        )
        
        val detectedValues = AdvancedPatternAnalyzer.analyzeBinaryData(binaryData)
        
        assertTrue("Should detect some values in binary data", detectedValues.isNotEmpty())
        
        // Check that reasonable values were detected
        val reasonableValues = detectedValues.filter { 
            val value = it.value.toLongOrNull() ?: 0
            value in 1..1000000
        }
        assertTrue("Should detect reasonable game values", reasonableValues.isNotEmpty())
    }

    @Test
    fun testAnalyzeTextContent() {
        val textContent = """
            player_gold=5000
            player_level=25
            high_score=98750
            sound_enabled=true
            music_volume=0.8
        """.trimIndent()
        
        val detectedValues = AdvancedPatternAnalyzer.analyzeTextContent(textContent)
        
        assertTrue("Should detect values in text content", detectedValues.isNotEmpty())
        
        // Check for specific detected values
        val goldValue = detectedValues.find { it.key == "player_gold" }
        assertNotNull("Should detect gold value", goldValue)
        assertEquals("Gold value should be correct", "5000", goldValue?.value)
        
        val levelValue = detectedValues.find { it.key == "player_level" }
        assertNotNull("Should detect level value", levelValue)
        assertEquals("Level value should be correct", "25", levelValue?.value)
        
        val scoreValue = detectedValues.find { it.key == "high_score" }
        assertNotNull("Should detect score value", scoreValue)
        assertEquals("Score value should be correct", "98750", scoreValue?.value)
    }

    @Test
    fun testContextualAnalysis() {
        val playerContext = "This is player data with game statistics"
        val gameContext = "Game save file with progress information"
        val saveContext = "Save data containing user progress"
        
        // Test with player context
        val goldWithPlayerContext = AdvancedPatternAnalyzer.analyzeKeyValue("gold", 1000, playerContext)
        val goldWithoutContext = AdvancedPatternAnalyzer.analyzeKeyValue("gold", 1000)
        
        if (goldWithPlayerContext != null && goldWithoutContext != null) {
            assertTrue("Context should improve confidence", 
                goldWithPlayerContext.confidence >= goldWithoutContext.confidence)
        }
        
        // Test with game context
        val levelWithGameContext = AdvancedPatternAnalyzer.analyzeKeyValue("level", 25, gameContext)
        assertNotNull("Should detect level with game context", levelWithGameContext)
        
        // Test with save context
        val expWithSaveContext = AdvancedPatternAnalyzer.analyzeKeyValue("experience", 5000, saveContext)
        assertNotNull("Should detect experience with save context", expWithSaveContext)
    }

    @Test
    fun testEdgeCases() {
        // Test null value
        val nullValue = AdvancedPatternAnalyzer.analyzeKeyValue("test", null)
        assertNull("Should not detect null values", nullValue)
        
        // Test empty key
        val emptyKeyValue = AdvancedPatternAnalyzer.analyzeKeyValue("", 123)
        assertNull("Should not detect empty key values", emptyKeyValue)
        
        // Test negative values
        val negativeValue = AdvancedPatternAnalyzer.analyzeKeyValue("player_gold", -100)
        if (negativeValue != null) {
            assertTrue("Negative values should have low confidence", negativeValue.confidence < 0.5)
        }
        
        // Test very small values
        val tinyValue = AdvancedPatternAnalyzer.analyzeKeyValue("player_level", 0)
        if (tinyValue != null) {
            assertTrue("Zero values should have reduced confidence", tinyValue.confidence < 0.8)
        }
    }
}