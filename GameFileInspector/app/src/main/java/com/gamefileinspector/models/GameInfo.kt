package com.gamefileinspector.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.io.File

@Parcelize
data class GameInfo(
    val packageName: String,
    val appName: String,
    val dataPath: String,
    val externalDataPath: String?,
    val obbPath: String?,
    val hasAccessibleData: Boolean,
    val gameFiles: List<GameFile> = emptyList()
) : Parcelable

@Parcelize
data class GameFile(
    val name: String,
    val path: String,
    val size: Long,
    val lastModified: Long,
    val type: FileType,
    val isReadable: Boolean,
    val isWritable: Boolean
) : Parcelable

enum class FileType {
    SAVE_FILE,
    CONFIG_FILE,
    DATABASE,
    BINARY,
    JSON,
    XML,
    PREFERENCES,
    UNKNOWN
}

data class FileAnalysis(
    val file: GameFile,
    val encoding: String?,
    val structure: FileStructure,
    val possibleValues: List<PossibleValue> = emptyList()
)

sealed class FileStructure {
    object Binary : FileStructure()
    object PlainText : FileStructure()
    object BINARY : FileStructure()
    object TEXT : FileStructure()
    object JSON : FileStructure()
    object XML : FileStructure()
    object KEY_VALUE : FileStructure()
    object DATABASE : FileStructure()
    object UNKNOWN : FileStructure()
    data class Json(val keys: List<String>) : FileStructure()
    data class Xml(val elements: List<String>) : FileStructure()
    data class Database(val tables: List<String>) : FileStructure()
    data class KeyValue(val pairs: Map<String, String>) : FileStructure()
}

data class PossibleValue(
    val offset: Long,
    val originalValue: String,
    val dataType: DataType,
    val description: String?,
    val confidence: Float
)

enum class DataType {
    INTEGER,
    FLOAT,
    STRING,
    BOOLEAN,
    CURRENCY,
    SCORE,
    LEVEL,
    EXPERIENCE,
    UNKNOWN
}