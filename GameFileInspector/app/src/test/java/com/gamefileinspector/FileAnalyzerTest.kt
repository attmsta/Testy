package com.gamefileinspector

import com.gamefileinspector.analyzers.FileAnalyzer
import com.gamefileinspector.models.DataType
import com.gamefileinspector.models.FileStructure
import com.gamefileinspector.models.GameFile
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.io.FileWriter

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class FileAnalyzerTest {

    private lateinit var testDir: File
    private lateinit var jsonFile: File
    private lateinit var propertiesFile: File
    private lateinit var binaryFile: File

    @Before
    fun setUp() {
        testDir = File.createTempFile("test", "dir").apply {
            delete()
            mkdirs()
        }
        
        // Create test JSON file
        jsonFile = File(testDir, "test_game.json")
        FileWriter(jsonFile).use { writer ->
            writer.write("""
                {
                    "player": {
                        "name": "TestPlayer",
                        "level": 25,
                        "gold": 12500,
                        "experience": 15750,
                        "gems": 150
                    },
                    "settings": {
                        "soundEnabled": true,
                        "musicVolume": 0.8,
                        "difficulty": "normal"
                    }
                }
            """.trimIndent())
        }
        
        // Create test properties file
        propertiesFile = File(testDir, "game_config.properties")
        FileWriter(propertiesFile).use { writer ->
            writer.write("""
                player.gold=5000
                player.level=10
                player.experience=2500
                sound.enabled=true
                graphics.quality=high
            """.trimIndent())
        }
        
        // Create test binary file
        binaryFile = File(testDir, "save_data.dat")
        val binaryData = byteArrayOf(
            0x00, 0x00, 0x13, 0x88.toByte(), // 5000 as 32-bit int
            0x00, 0x00, 0x00, 0x0A, // 10 as 32-bit int
            0x00, 0x00, 0x09, 0xC4.toByte(), // 2500 as 32-bit int
            0x01, // boolean true
            0x00, 0x00, 0x00, 0x64 // 100 as 32-bit int
        )
        binaryFile.writeBytes(binaryData)
    }

    @Test
    fun testAnalyzeJsonFile() {
        val gameFile = GameFile(
            path = jsonFile.absolutePath,
            name = jsonFile.name,
            size = jsonFile.length(),
            lastModified = jsonFile.lastModified(),
            gamePackage = "com.test.game",
            isReadable = true,
            isWritable = true
        )
        
        val analysis = FileAnalyzer.analyzeFile(gameFile)
        
        assertNotNull("Analysis should not be null", analysis)
        assertEquals("Should detect JSON structure", FileStructure.JSON, analysis.structure)
        assertTrue("Should detect possible values", analysis.possibleValues.isNotEmpty())
        
        // Check for specific values
        val goldValue = analysis.possibleValues.find { it.key == "gold" }
        assertNotNull("Should detect gold value", goldValue)
        assertEquals("Gold value should be correct", "12500", goldValue?.value)
        assertEquals("Gold should be integer type", DataType.INTEGER, goldValue?.dataType)
        assertTrue("Gold confidence should be high", goldValue?.confidence ?: 0.0 > 0.7)
        
        val levelValue = analysis.possibleValues.find { it.key == "level" }
        assertNotNull("Should detect level value", levelValue)
        assertEquals("Level value should be correct", "25", levelValue?.value)
        
        val experienceValue = analysis.possibleValues.find { it.key == "experience" }
        assertNotNull("Should detect experience value", experienceValue)
        assertEquals("Experience value should be correct", "15750", experienceValue?.value)
    }

    @Test
    fun testAnalyzePropertiesFile() {
        val gameFile = GameFile(
            path = propertiesFile.absolutePath,
            name = propertiesFile.name,
            size = propertiesFile.length(),
            lastModified = propertiesFile.lastModified(),
            gamePackage = "com.test.game",
            isReadable = true,
            isWritable = true
        )
        
        val analysis = FileAnalyzer.analyzeFile(gameFile)
        
        assertNotNull("Analysis should not be null", analysis)
        assertEquals("Should detect KEY_VALUE structure", FileStructure.KEY_VALUE, analysis.structure)
        assertTrue("Should detect possible values", analysis.possibleValues.isNotEmpty())
        
        // Check for specific properties
        val goldProperty = analysis.possibleValues.find { it.key == "player.gold" }
        assertNotNull("Should detect gold property", goldProperty)
        assertEquals("Gold property value should be correct", "5000", goldProperty?.value)
        
        val levelProperty = analysis.possibleValues.find { it.key == "player.level" }
        assertNotNull("Should detect level property", levelProperty)
        assertEquals("Level property value should be correct", "10", levelProperty?.value)
    }

    @Test
    fun testAnalyzeBinaryFile() {
        val gameFile = GameFile(
            path = binaryFile.absolutePath,
            name = binaryFile.name,
            size = binaryFile.length(),
            lastModified = binaryFile.lastModified(),
            gamePackage = "com.test.game",
            isReadable = true,
            isWritable = true
        )
        
        val analysis = FileAnalyzer.analyzeFile(gameFile)
        
        assertNotNull("Analysis should not be null", analysis)
        assertEquals("Should detect BINARY structure", FileStructure.BINARY, analysis.structure)
        assertTrue("Should detect possible values", analysis.possibleValues.isNotEmpty())
        
        // Check that some integer values were detected
        val integerValues = analysis.possibleValues.filter { it.dataType == DataType.INTEGER }
        assertTrue("Should detect integer values in binary data", integerValues.isNotEmpty())
    }

    @Test
    fun testDetectFileStructure() {
        assertEquals("Should detect JSON", FileStructure.JSON, FileAnalyzer.detectFileStructure(jsonFile.absolutePath))
        assertEquals("Should detect KEY_VALUE", FileStructure.KEY_VALUE, FileAnalyzer.detectFileStructure(propertiesFile.absolutePath))
        assertEquals("Should detect BINARY", FileStructure.BINARY, FileAnalyzer.detectFileStructure(binaryFile.absolutePath))
    }

    @Test
    fun testDetectEncoding() {
        val encoding = FileAnalyzer.detectEncoding(jsonFile.absolutePath)
        assertTrue("Should detect UTF-8 or ASCII encoding", 
            encoding == "UTF-8" || encoding == "ASCII" || encoding == "US-ASCII")
    }

    @Test
    fun testAnalyzeNonExistentFile() {
        val nonExistentFile = GameFile(
            path = "/non/existent/file.json",
            name = "file.json",
            size = 0,
            lastModified = 0,
            gamePackage = "com.test.game",
            isReadable = false,
            isWritable = false
        )
        
        try {
            FileAnalyzer.analyzeFile(nonExistentFile)
            fail("Should throw exception for non-existent file")
        } catch (e: Exception) {
            // Expected behavior
            assertTrue("Should be a meaningful exception", e.message?.isNotEmpty() == true)
        }
    }

    @Test
    fun testAnalyzeEmptyFile() {
        val emptyFile = File(testDir, "empty.json")
        emptyFile.createNewFile()
        
        val gameFile = GameFile(
            path = emptyFile.absolutePath,
            name = emptyFile.name,
            size = emptyFile.length(),
            lastModified = emptyFile.lastModified(),
            gamePackage = "com.test.game",
            isReadable = true,
            isWritable = true
        )
        
        val analysis = FileAnalyzer.analyzeFile(gameFile)
        
        assertNotNull("Analysis should not be null for empty file", analysis)
        assertTrue("Empty file should have no possible values", analysis.possibleValues.isEmpty())
    }

    @Test
    fun testAnalyzeCorruptedJsonFile() {
        val corruptedJsonFile = File(testDir, "corrupted.json")
        FileWriter(corruptedJsonFile).use { writer ->
            writer.write("{ invalid json content }")
        }
        
        val gameFile = GameFile(
            path = corruptedJsonFile.absolutePath,
            name = corruptedJsonFile.name,
            size = corruptedJsonFile.length(),
            lastModified = corruptedJsonFile.lastModified(),
            gamePackage = "com.test.game",
            isReadable = true,
            isWritable = true
        )
        
        val analysis = FileAnalyzer.analyzeFile(gameFile)
        
        assertNotNull("Analysis should not be null for corrupted JSON", analysis)
        // Should fallback to text analysis
        assertEquals("Should fallback to TEXT structure", FileStructure.TEXT, analysis.structure)
    }
}