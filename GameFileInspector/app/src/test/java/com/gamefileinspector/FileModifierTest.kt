package com.gamefileinspector

import com.gamefileinspector.models.DataType
import com.gamefileinspector.models.GameFile
import com.gamefileinspector.models.PossibleValue
import com.gamefileinspector.utils.FileModifier
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
class FileModifierTest {

    private lateinit var testDir: File
    private lateinit var jsonFile: File
    private lateinit var propertiesFile: File

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
                        "experience": 15750
                    },
                    "settings": {
                        "soundEnabled": true,
                        "musicVolume": 0.8
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
            """.trimIndent())
        }
    }

    @Test
    fun testModifyJsonIntegerValue() {
        val gameFile = GameFile(
            path = jsonFile.absolutePath,
            name = jsonFile.name,
            size = jsonFile.length(),
            lastModified = jsonFile.lastModified(),
            gamePackage = "com.test.game",
            isReadable = true,
            isWritable = true
        )
        
        val goldValue = PossibleValue(
            key = "gold",
            value = "99999",
            dataType = DataType.INTEGER,
            confidence = 0.9,
            description = "Player gold",
            location = "player.gold",
            category = "currency"
        )
        
        val result = FileModifier.modifyValue(gameFile, goldValue)
        assertTrue("Modification should succeed", result)
        
        // Verify the change
        val modifiedContent = jsonFile.readText()
        assertTrue("File should contain new gold value", modifiedContent.contains("99999"))
        assertFalse("File should not contain old gold value", modifiedContent.contains("12500"))
    }

    @Test
    fun testModifyJsonFloatValue() {
        val gameFile = GameFile(
            path = jsonFile.absolutePath,
            name = jsonFile.name,
            size = jsonFile.length(),
            lastModified = jsonFile.lastModified(),
            gamePackage = "com.test.game",
            isReadable = true,
            isWritable = true
        )
        
        val volumeValue = PossibleValue(
            key = "musicVolume",
            value = "1.0",
            dataType = DataType.FLOAT,
            confidence = 0.8,
            description = "Music volume",
            location = "settings.musicVolume",
            category = "settings"
        )
        
        val result = FileModifier.modifyValue(gameFile, volumeValue)
        assertTrue("Modification should succeed", result)
        
        // Verify the change
        val modifiedContent = jsonFile.readText()
        assertTrue("File should contain new volume value", modifiedContent.contains("1.0"))
        assertFalse("File should not contain old volume value", modifiedContent.contains("0.8"))
    }

    @Test
    fun testModifyJsonBooleanValue() {
        val gameFile = GameFile(
            path = jsonFile.absolutePath,
            name = jsonFile.name,
            size = jsonFile.length(),
            lastModified = jsonFile.lastModified(),
            gamePackage = "com.test.game",
            isReadable = true,
            isWritable = true
        )
        
        val soundValue = PossibleValue(
            key = "soundEnabled",
            value = "false",
            dataType = DataType.BOOLEAN,
            confidence = 0.9,
            description = "Sound enabled",
            location = "settings.soundEnabled",
            category = "settings"
        )
        
        val result = FileModifier.modifyValue(gameFile, soundValue)
        assertTrue("Modification should succeed", result)
        
        // Verify the change
        val modifiedContent = jsonFile.readText()
        assertTrue("File should contain new boolean value", modifiedContent.contains("false"))
    }

    @Test
    fun testModifyPropertiesValue() {
        val gameFile = GameFile(
            path = propertiesFile.absolutePath,
            name = propertiesFile.name,
            size = propertiesFile.length(),
            lastModified = propertiesFile.lastModified(),
            gamePackage = "com.test.game",
            isReadable = true,
            isWritable = true
        )
        
        val goldProperty = PossibleValue(
            key = "player.gold",
            value = "50000",
            dataType = DataType.INTEGER,
            confidence = 0.9,
            description = "Player gold",
            location = "player.gold",
            category = "currency"
        )
        
        val result = FileModifier.modifyValue(gameFile, goldProperty)
        assertTrue("Modification should succeed", result)
        
        // Verify the change
        val modifiedContent = propertiesFile.readText()
        assertTrue("File should contain new gold value", modifiedContent.contains("player.gold=50000"))
        assertFalse("File should not contain old gold value", modifiedContent.contains("player.gold=5000"))
    }

    @Test
    fun testModifyNonExistentFile() {
        val nonExistentFile = GameFile(
            path = "/non/existent/file.json",
            name = "file.json",
            size = 0,
            lastModified = 0,
            gamePackage = "com.test.game",
            isReadable = false,
            isWritable = false
        )
        
        val testValue = PossibleValue(
            key = "test",
            value = "123",
            dataType = DataType.INTEGER,
            confidence = 0.9,
            description = "Test value",
            location = "test",
            category = "test"
        )
        
        val result = FileModifier.modifyValue(nonExistentFile, testValue)
        assertFalse("Modification should fail for non-existent file", result)
    }

    @Test
    fun testModifyReadOnlyFile() {
        // Make file read-only
        jsonFile.setReadOnly()
        
        val gameFile = GameFile(
            path = jsonFile.absolutePath,
            name = jsonFile.name,
            size = jsonFile.length(),
            lastModified = jsonFile.lastModified(),
            gamePackage = "com.test.game",
            isReadable = true,
            isWritable = false
        )
        
        val testValue = PossibleValue(
            key = "gold",
            value = "99999",
            dataType = DataType.INTEGER,
            confidence = 0.9,
            description = "Player gold",
            location = "player.gold",
            category = "currency"
        )
        
        val result = FileModifier.modifyValue(gameFile, testValue)
        assertFalse("Modification should fail for read-only file", result)
    }

    @Test
    fun testValidateValue() {
        // Test integer validation
        assertTrue("Valid integer should pass", FileModifier.validateValue("123", DataType.INTEGER))
        assertFalse("Invalid integer should fail", FileModifier.validateValue("abc", DataType.INTEGER))
        assertFalse("Float as integer should fail", FileModifier.validateValue("12.34", DataType.INTEGER))
        
        // Test float validation
        assertTrue("Valid float should pass", FileModifier.validateValue("12.34", DataType.FLOAT))
        assertTrue("Integer as float should pass", FileModifier.validateValue("123", DataType.FLOAT))
        assertFalse("Invalid float should fail", FileModifier.validateValue("abc", DataType.FLOAT))
        
        // Test boolean validation
        assertTrue("'true' should pass", FileModifier.validateValue("true", DataType.BOOLEAN))
        assertTrue("'false' should pass", FileModifier.validateValue("false", DataType.BOOLEAN))
        assertFalse("Invalid boolean should fail", FileModifier.validateValue("maybe", DataType.BOOLEAN))
        
        // Test string validation (should always pass)
        assertTrue("Any string should pass", FileModifier.validateValue("any string", DataType.STRING))
        assertTrue("Empty string should pass", FileModifier.validateValue("", DataType.STRING))
    }

    @Test
    fun testModifyWithInvalidValue() {
        val gameFile = GameFile(
            path = jsonFile.absolutePath,
            name = jsonFile.name,
            size = jsonFile.length(),
            lastModified = jsonFile.lastModified(),
            gamePackage = "com.test.game",
            isReadable = true,
            isWritable = true
        )
        
        val invalidValue = PossibleValue(
            key = "gold",
            value = "not_a_number",
            dataType = DataType.INTEGER,
            confidence = 0.9,
            description = "Player gold",
            location = "player.gold",
            category = "currency"
        )
        
        val result = FileModifier.modifyValue(gameFile, invalidValue)
        assertFalse("Modification should fail for invalid value", result)
        
        // Verify file wasn't changed
        val content = jsonFile.readText()
        assertTrue("File should still contain original value", content.contains("12500"))
    }

    @Test
    fun testModifyNestedJsonValue() {
        val gameFile = GameFile(
            path = jsonFile.absolutePath,
            name = jsonFile.name,
            size = jsonFile.length(),
            lastModified = jsonFile.lastModified(),
            gamePackage = "com.test.game",
            isReadable = true,
            isWritable = true
        )
        
        val levelValue = PossibleValue(
            key = "level",
            value = "50",
            dataType = DataType.INTEGER,
            confidence = 0.9,
            description = "Player level",
            location = "player.level",
            category = "progress"
        )
        
        val result = FileModifier.modifyValue(gameFile, levelValue)
        assertTrue("Modification should succeed", result)
        
        // Verify the change
        val modifiedContent = jsonFile.readText()
        assertTrue("File should contain new level value", modifiedContent.contains("\"level\": 50"))
        assertFalse("File should not contain old level value", modifiedContent.contains("\"level\": 25"))
    }
}