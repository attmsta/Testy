# Game File Inspector

[![Build Status](https://img.shields.io/badge/build-passing-brightgreen)](https://github.com/photoparsley/test/actions)
[![Release](https://img.shields.io/badge/release-latest-blue)](https://github.com/photoparsley/test/releases)
[![License](https://img.shields.io/badge/license-MIT-green)](LICENSE)
[![Android](https://img.shields.io/badge/platform-Android%207.0%2B-green)](https://developer.android.com)

A powerful Android application for reverse engineering and modifying game files without requiring root access. This app allows users to analyze and modify game save files, configuration files, and other accessible game data to change values like gold coins, levels, experience points, and more.

## 🚀 Automated Builds & Releases

This project features comprehensive build automation that creates APK releases automatically:

- ✅ **Automatic APK Generation** on every push to main branch
- ✅ **GitHub Releases** with downloadable APK files  
- ✅ **GitLab CI/CD** with comprehensive testing and deployment
- ✅ **Local Build Scripts** for development and testing
- ✅ **Quality Assurance** with automated testing and security scanning

### 📥 Download Latest Release

**[📱 Download Latest APK](https://github.com/photoparsley/test/releases/latest)** - Always up-to-date with the latest features

### 🔄 Build Status

| Platform | Status | Download |
|----------|--------|----------|
| GitHub Actions | ![Build](https://img.shields.io/badge/build-automated-success) | [Latest Release](https://github.com/photoparsley/test/releases) |
| GitLab CI/CD | ![Pipeline](https://img.shields.io/badge/pipeline-automated-success) | [Pipeline Artifacts](https://gitlab.com/photoparsley/test/-/pipelines) |
| Local Build | ![Script](https://img.shields.io/badge/script-ready-success) | `./build_apk.sh` |

## Features

### 🔍 **File Analysis**
- **Smart Game Detection**: Automatically scans for installed games and their accessible data files
- **Multi-Format Support**: Handles JSON, XML, binary, database, key-value, and plain text files
- **Intelligent Value Detection**: Uses pattern recognition to identify potential game values (currency, scores, levels, etc.)
- **Confidence Scoring**: Provides confidence ratings for detected values

### 🛠️ **File Modification**
- **Safe Value Editing**: Modify detected game values with type validation
- **Multiple Data Types**: Support for integers, floats, strings, and boolean values
- **Backup System**: Automatic file backup before modifications with restore capability
- **Hex Editor**: Built-in hex editor for advanced binary file editing

### 🔒 **Security & Safety**
- **Rootless Operation**: Works without root permissions using standard Android storage access
- **File Validation**: Ensures file integrity and prevents corruption
- **Permission Management**: Proper handling of storage permissions
- **Backup Management**: Keeps multiple backup versions with automatic cleanup

## Supported File Types

| Type | Extensions | Description |
|------|------------|-------------|
| **JSON** | `.json` | Game save data, configuration files |
| **XML** | `.xml` | Android preferences, game settings |
| **Database** | `.db`, `.sqlite` | SQLite game databases |
| **Key-Value** | `.properties`, `.ini`, `.cfg` | Configuration files |
| **Binary** | `.dat`, `.sav`, `.bin` | Binary save files |
| **Text** | `.txt`, `.log` | Plain text data files |

## Installation

1. **Download APK**: Get the latest APK from the releases page
2. **Enable Unknown Sources**: Allow installation from unknown sources in Android settings
3. **Install**: Install the APK file
4. **Grant Permissions**: Allow storage access when prompted

## Usage

### 1. **Scan for Games**
- Launch the app and tap "Scan Games"
- The app will automatically detect installed games with accessible data
- Games are categorized by their data accessibility

### 2. **Analyze Game Files**
- Select a game from the list
- Browse through the detected game files
- Tap on a file to analyze its contents

### 3. **Modify Values**
- Review the detected possible values
- Tap on a value you want to modify
- Enter the new value and confirm
- The app will automatically backup the original file

### 4. **Advanced Editing**
- Use the hex editor for binary files
- Create manual backups before major changes
- Restore from backups if needed

## Technical Details

### Architecture
- **Language**: Kotlin
- **UI Framework**: Android Views with Material Design
- **Architecture Pattern**: Activity-based with RecyclerView adapters
- **Concurrency**: Kotlin Coroutines for background operations
- **File I/O**: Standard Java/Kotlin file operations

### Key Components
- **GameScanner**: Detects installed games and accessible files
- **FileAnalyzer**: Analyzes file formats and detects game values
- **FileModifier**: Safely modifies file contents
- **FileBackupManager**: Handles backup and restore operations
- **HexEditor**: Provides hex editing capabilities

### Permissions Required
- `READ_EXTERNAL_STORAGE`: Read game files
- `WRITE_EXTERNAL_STORAGE`: Modify game files
- `MANAGE_EXTERNAL_STORAGE`: Access all files (Android 11+)
- `QUERY_ALL_PACKAGES`: List installed games

## Supported Android Versions

- **Minimum SDK**: Android 7.0 (API 24)
- **Target SDK**: Android 14 (API 34)
- **Recommended**: Android 8.0+ for best performance

## File Detection Patterns

The app uses intelligent pattern recognition to identify game values:

### Currency Detection
- JSON keys: `gold`, `coins`, `money`, `cash`, `currency`
- Patterns: Large integer values (>100)
- Context: Economic game elements

### Level/Progress Detection
- JSON keys: `level`, `stage`, `progress`, `rank`
- Patterns: Small to medium integers (1-1000)
- Context: Progression systems

### Experience Points
- JSON keys: `exp`, `experience`, `xp`
- Patterns: Medium to large integers
- Context: Character progression

### Score Detection
- JSON keys: `score`, `points`, `highscore`
- Patterns: Large integer values
- Context: Achievement systems

## Safety Features

### Automatic Backups
- Creates timestamped backups before any modification
- Keeps up to 5 recent backups per file
- Easy one-click restore functionality

### File Validation
- Checks file permissions before modification
- Validates data types and ranges
- Prevents corruption through type checking

### Error Handling
- Comprehensive error reporting
- Safe failure modes
- Transaction-like operations

## Limitations

### Root-Free Constraints
- Can only access files in public storage areas
- Cannot modify system-protected game files
- Limited to games that store data in accessible locations

### File Format Support
- Some proprietary formats may not be supported
- Encrypted files cannot be modified
- Compressed archives require extraction

### Game Compatibility
- Modern games with server-side validation may reset values
- Some games detect file modifications
- Online games may have additional protection

## Development

### Building from Source
```bash
git clone <repository-url>
cd GameFileInspector
./gradlew assembleDebug
```

### Project Structure
```
app/src/main/
├── java/com/gamefileinspector/
│   ├── activities/          # UI Activities
│   ├── adapters/           # RecyclerView Adapters
│   ├── models/             # Data Models
│   ├── utils/              # Utility Classes
│   └── dialogs/            # Dialog Fragments
├── res/
│   ├── layout/             # XML Layouts
│   ├── values/             # Strings, Colors, Themes
│   ├── drawable/           # Icons and Graphics
│   └── menu/               # Menu Resources
└── AndroidManifest.xml     # App Configuration
```

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests if applicable
5. Submit a pull request

## License

This project is licensed under the MIT License - see the LICENSE file for details.

## Disclaimer

This application is for educational and research purposes. Users are responsible for complying with game terms of service and applicable laws. The developers are not responsible for any consequences of using this application to modify game files.

## Support

For issues, feature requests, or questions:
- Create an issue on GitHub
- Check the FAQ section
- Review existing documentation

---

**Note**: This application works entirely within Android's permission system and does not require root access. It can only modify files that are accessible through standard Android storage permissions.