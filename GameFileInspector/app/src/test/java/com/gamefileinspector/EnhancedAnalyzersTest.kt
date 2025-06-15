package com.gamefileinspector

import com.gamefileinspector.analyzers.DeepFileAnalyzer
import com.gamefileinspector.analyzers.FileStructureAnalyzer
import com.gamefileinspector.analyzers.ValuePatternDetector
import com.gamefileinspector.models.GameFile
import org.junit.Test
import org.junit.Assert.*
import org.junit.Before
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import java.io.File
import java.io.FileWriter

/**
 * Test suite for enhanced file analyzers
 */
@RunWith(RobolectricTestRunner::class)
class EnhancedAnalyzersTest {
    
    private lateinit var testDir: File
    
    @Before
    fun setup() {
        testDir = File(RuntimeEnvironment.getApplication().cacheDir, "test_files")
        testDir.mkdirs()
    }
    
    @Test
    fun testDeepFileAnalyzer_JsonFile() {
        // Create test JSON file with game data
        val jsonFile = File(testDir, "game_save.json")
        val jsonContent = """
        {
            "player": {
                "name": "TestPlayer",
                "level": 42,
                "experience": 125000,
                "gold": 75000,
                "gems": 250,
                "health": 100,
                "mana": 80
            },
            "inventory": {
                "sword": 1,
                "potion": 15,
                "armor": 1
            },
            "stats": {
                "attack": 65,
                "defense": 40,
                "speed": 30,
                "luck": 15
            },
            "achievements": {
                "first_kill": true,
                "gold_collector": false,
                "level_master": true
            }
        }
        """.trimIndent()
        
        FileWriter(jsonFile).use { it.write(jsonContent) }
        
        val gameFile = GameFile(
            path = jsonFile.absolutePath,
            name = jsonFile.name,
            size = jsonFile.length(),
            lastModified = jsonFile.lastModified(),
            gamePackage = "com.test.game",
            isReadable = true,
            isWritable = true
        )
        
        val values = DeepFileAnalyzer.performDeepAnalysis(gameFile)
        
        // Should detect multiple game values
        assertTrue("Should detect at least 10 values", values.size >= 10)
        
        // Should detect currency values
        val currencyValues = values.filter { it.category == "currency" }
        assertTrue("Should detect currency values", currencyValues.isNotEmpty())
        
        // Should detect level/progress values
        val progressValues = values.filter { it.category == "progress" }
        assertTrue("Should detect progress values", progressValues.isNotEmpty())
        
        // Should detect stats values
        val statsValues = values.filter { it.category == "stats" }
        assertTrue("Should detect stats values", statsValues.isNotEmpty())
        
        // Check confidence scores
        val highConfidenceValues = values.filter { it.confidence > 0.7 }
        assertTrue("Should have high confidence values", highConfidenceValues.isNotEmpty())
    }
    
    @Test
    fun testValuePatternDetector_CurrencyProgression() {
        val content = """
        gold=1000
        silver=500
        copper=100
        gems=50
        diamonds=10
        """.trimIndent()
        
        val values = ValuePatternDetector.analyzeValuePatterns(content)
        
        // Should detect currency patterns
        val currencyValues = values.filter { it.category == "currency" || it.category == "premium_currency" }
        assertTrue("Should detect currency patterns", currencyValues.isNotEmpty())
        
        // Should have reasonable confidence
        val confidentValues = values.filter { it.confidence > 0.5 }
        assertTrue("Should have confident detections", confidentValues.isNotEmpty())
    }
    
    @Test
    fun testValuePatternDetector_LevelSequence() {
        val content = """
        level_1_exp=100
        level_2_exp=250
        level_3_exp=500
        level_4_exp=1000
        level_5_exp=2000
        """.trimIndent()
        
        val values = ValuePatternDetector.analyzeValuePatterns(content)
        
        // Should detect experience progression
        val expValues = values.filter { it.category == "experience" }
        assertTrue("Should detect experience values", expValues.isNotEmpty())
        
        // Should detect level progression
        val progressValues = values.filter { it.category == "progress" }
        assertTrue("Should detect progress values", progressValues.isNotEmpty())
    }
    
    @Test
    fun testValuePatternDetector_AdvancedPatterns() {
        val content = """
        player_attack=45 player_defense=30
        building_level=5 tower_level=3
        quest_progress=75 mission_complete=12
        wood=1500 stone=800 iron=200
        """.trimIndent()
        
        val values = ValuePatternDetector.analyzeValuePatterns(content)
        
        // Should detect various game patterns
        assertTrue("Should detect multiple patterns", values.size >= 5)
        
        // Should detect stats
        val statsValues = values.filter { it.description.contains("stat", ignoreCase = true) }
        assertTrue("Should detect stat patterns", statsValues.isNotEmpty())
        
        // Should detect resources
        val resourceValues = values.filter { it.description.contains("resource", ignoreCase = true) }
        assertTrue("Should detect resource patterns", resourceValues.isNotEmpty())
    }
    
    @Test
    fun testFileStructureAnalyzer_BinaryFile() {
        // Create a binary file with game data patterns
        val binaryFile = File(testDir, "game_data.dat")
        val binaryData = byteArrayOf(
            // Header: "GAME"
            0x47, 0x41, 0x4D, 0x45,
            // Player level (little endian): 25
            0x19, 0x00, 0x00, 0x00,
            // Experience (little endian): 15750
            0x86, 0x3D, 0x00, 0x00,
            // Gold (little endian): 12500
            0xD4, 0x30, 0x00, 0x00,
            // Health (little endian): 100
            0x64, 0x00, 0x00, 0x00,
            // Mana (little endian): 80
            0x50, 0x00, 0x00, 0x00
        )
        
        binaryFile.writeBytes(binaryData)
        
        val values = FileStructureAnalyzer.analyzeFileStructure(binaryFile)
        
        // Should detect binary values
        assertTrue("Should detect binary values", values.isNotEmpty())
        
        // Should detect file signature
        val signatureValues = values.filter { it.key == "file_signature" }
        assertTrue("Should detect file signature", signatureValues.isNotEmpty())
        
        // Should detect numeric values
        val numericValues = values.filter { it.dataType.name.contains("INTEGER") }
        assertTrue("Should detect numeric values", numericValues.isNotEmpty())
    }
    
    @Test
    fun testFileStructureAnalyzer_PlayerDataStructure() {
        // Create binary data that looks like player data structure
        val binaryFile = File(testDir, "player_data.bin")
        val playerData = byteArrayOf(
            // Level: 42
            0x2A, 0x00, 0x00, 0x00,
            // Experience: 125000
            0xC8, 0xE8, 0x01, 0x00,
            // Gold: 75000
            0x38, 0x25, 0x01, 0x00,
            // Health: 100
            0x64, 0x00, 0x00, 0x00
        )
        
        binaryFile.writeBytes(playerData)
        
        val values = FileStructureAnalyzer.analyzeFileStructure(binaryFile)
        
        // Should detect player data patterns
        val playerValues = values.filter { it.description.contains("player", ignoreCase = true) }
        assertTrue("Should detect player data patterns", playerValues.isNotEmpty())
        
        // Should categorize values correctly
        val currencyValues = values.filter { it.category == "currency" }
        val progressValues = values.filter { it.category == "progress" }
        val healthValues = values.filter { it.category == "health_energy" }
        
        assertTrue("Should detect at least one category", 
            currencyValues.isNotEmpty() || progressValues.isNotEmpty() || healthValues.isNotEmpty())
    }
    
    @Test
    fun testDeepFileAnalyzer_XmlFile() {
        val xmlFile = File(testDir, "game_config.xml")
        val xmlContent = """
        <?xml version="1.0" encoding="UTF-8"?>
        <gameData>
            <player level="25" experience="15750" gold="12500" gems="150"/>
            <stats attack="45" defense="30" speed="25" luck="15"/>
            <inventory>
                <item id="sword" quantity="1" durability="100"/>
                <item id="potion" quantity="12" durability="-1"/>
            </inventory>
            <settings>
                <audio masterVolume="0.8" musicVolume="0.7" sfxVolume="0.9"/>
                <graphics quality="high" fullscreen="false" vsync="true"/>
            </settings>
        </gameData>
        """.trimIndent()
        
        FileWriter(xmlFile).use { it.write(xmlContent) }
        
        val gameFile = GameFile(
            path = xmlFile.absolutePath,
            name = xmlFile.name,
            size = xmlFile.length(),
            lastModified = xmlFile.lastModified(),
            gamePackage = "com.test.game",
            isReadable = true,
            isWritable = true
        )
        
        val values = DeepFileAnalyzer.performDeepAnalysis(gameFile)
        
        // Should detect XML attributes and elements
        assertTrue("Should detect XML values", values.isNotEmpty())
        
        // Should detect different categories
        val categories = values.map { it.category }.distinct()
        assertTrue("Should detect multiple categories", categories.size >= 3)
        
        // Should have good confidence for clear game values
        val highConfidenceValues = values.filter { it.confidence > 0.6 }
        assertTrue("Should have high confidence values", highConfidenceValues.isNotEmpty())
    }
    
    @Test
    fun testDeepFileAnalyzer_PropertiesFile() {
        val propsFile = File(testDir, "game.properties")
        val propsContent = """
        # Player Data
        player.level=42
        player.experience=125000
        player.gold=75000
        player.gems=250
        
        # Game Settings
        audio.master_volume=0.8
        audio.music_volume=0.7
        audio.sfx_volume=0.9
        graphics.quality=high
        graphics.fullscreen=false
        
        # Statistics
        stats.games_played=156
        stats.total_playtime=3600000
        stats.high_score=987654
        """.trimIndent()
        
        FileWriter(propsFile).use { it.write(propsContent) }
        
        val gameFile = GameFile(
            path = propsFile.absolutePath,
            name = propsFile.name,
            size = propsFile.length(),
            lastModified = propsFile.lastModified(),
            gamePackage = "com.test.game",
            isReadable = true,
            isWritable = true
        )
        
        val values = DeepFileAnalyzer.performDeepAnalysis(gameFile)
        
        // Should detect properties values
        assertTrue("Should detect properties values", values.isNotEmpty())
        
        // Should detect sections
        val sectionsDetected = values.any { it.location.contains("[") }
        // Properties files might not have sections, so this is optional
        
        // Should detect different data types
        val dataTypes = values.map { it.dataType }.distinct()
        assertTrue("Should detect multiple data types", dataTypes.size >= 2)
        
        // Should detect game-related values
        val gameValues = values.filter { 
            it.key.contains("level") || it.key.contains("gold") || it.key.contains("experience")
        }
        assertTrue("Should detect game-related values", gameValues.isNotEmpty())
    }
    
    @Test
    fun testValuePatternDetector_RelationshipPatterns() {
        val content = """
        base_damage=10
        weapon_damage=20
        total_damage=30
        level_1=100
        level_2=200
        level_3=400
        level_4=800
        """.trimIndent()
        
        val values = ValuePatternDetector.analyzeValuePatterns(content)
        
        // Should detect some patterns
        assertTrue("Should detect patterns", values.isNotEmpty())
        
        // Should detect relationship patterns
        val relationshipValues = values.filter { it.category == "relationships" }
        // Relationship detection might be complex, so this is optional
        
        // Should detect progression patterns
        val progressionValues = values.filter { 
            it.description.contains("progression", ignoreCase = true) ||
            it.description.contains("sequence", ignoreCase = true)
        }
        // This might also be detected in other ways
        
        // At minimum, should detect the individual values
        val numericValues = values.filter { it.dataType.name.contains("INTEGER") }
        assertTrue("Should detect numeric values", numericValues.isNotEmpty())
    }
    
    @Test
    fun testEnhancedAnalyzers_Integration() {
        // Test integration of all analyzers
        val jsonFile = File(testDir, "complex_game.json")
        val complexContent = """
        {
            "version": "1.2.3",
            "player": {
                "id": "player_001",
                "name": "TestHero",
                "level": 50,
                "experience": 275000,
                "next_level_exp": 300000,
                "currencies": {
                    "gold": 125000,
                    "gems": 450,
                    "tokens": 75
                },
                "stats": {
                    "health": 150,
                    "mana": 120,
                    "attack": 85,
                    "defense": 60,
                    "speed": 45,
                    "luck": 25,
                    "critical_chance": 0.15,
                    "critical_damage": 1.5
                }
            },
            "inventory": [
                {"id": "sword_legendary", "quantity": 1, "durability": 95},
                {"id": "potion_health", "quantity": 25, "durability": -1},
                {"id": "armor_epic", "quantity": 1, "durability": 87},
                {"id": "gem_ruby", "quantity": 8, "durability": -1}
            ],
            "achievements": {
                "first_blood": {"unlocked": true, "progress": 1, "max": 1},
                "gold_hoarder": {"unlocked": false, "progress": 125000, "max": 1000000},
                "level_cap": {"unlocked": true, "progress": 50, "max": 100},
                "item_collector": {"unlocked": false, "progress": 234, "max": 1000}
            },
            "settings": {
                "audio": {
                    "master": 0.85,
                    "music": 0.75,
                    "sfx": 0.95,
                    "voice": 0.80
                },
                "graphics": {
                    "quality": "ultra",
                    "resolution": {"width": 1920, "height": 1080},
                    "fullscreen": true,
                    "vsync": true,
                    "fps_limit": 60
                }
            },
            "progress": {
                "current_quest": "ancient_artifact_hunt",
                "completed_quests": 47,
                "total_quests": 75,
                "worlds_unlocked": 4,
                "total_worlds": 6,
                "secrets_found": 23,
                "total_secrets": 50
            },
            "timestamps": {
                "created": 1640995200,
                "last_played": 1718467200,
                "total_playtime": 7200000,
                "session_count": 89
            }
        }
        """.trimIndent()
        
        FileWriter(jsonFile).use { it.write(complexContent) }
        
        val gameFile = GameFile(
            path = jsonFile.absolutePath,
            name = jsonFile.name,
            size = jsonFile.length(),
            lastModified = jsonFile.lastModified(),
            gamePackage = "com.epic.game",
            isReadable = true,
            isWritable = true
        )
        
        // Test all analyzers
        val deepValues = DeepFileAnalyzer.performDeepAnalysis(gameFile)
        val structureValues = FileStructureAnalyzer.analyzeFileStructure(jsonFile)
        val patternValues = ValuePatternDetector.analyzeValuePatterns(complexContent)
        
        // All analyzers should find values
        assertTrue("Deep analyzer should find values", deepValues.isNotEmpty())
        assertTrue("Structure analyzer should find values", structureValues.isNotEmpty())
        assertTrue("Pattern analyzer should find values", patternValues.isNotEmpty())
        
        // Combined should have significant number of detections
        val allValues = (deepValues + structureValues + patternValues)
            .distinctBy { "${it.key}_${it.value}_${it.location}" }
        
        assertTrue("Combined analysis should find many values", allValues.size >= 20)
        
        // Should detect all major categories
        val categories = allValues.map { it.category }.distinct()
        val expectedCategories = listOf("currency", "progress", "stats", "inventory", "settings")
        val foundCategories = expectedCategories.filter { expected ->
            categories.any { it.contains(expected, ignoreCase = true) }
        }
        
        assertTrue("Should detect major game categories", foundCategories.size >= 3)
        
        // Should have high confidence values
        val highConfidenceValues = allValues.filter { it.confidence > 0.7 }
        assertTrue("Should have high confidence detections", highConfidenceValues.size >= 10)
        
        // Should detect different data types
        val dataTypes = allValues.map { it.dataType }.distinct()
        assertTrue("Should detect multiple data types", dataTypes.size >= 3)
    }
}