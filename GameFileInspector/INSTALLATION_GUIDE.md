# Game File Inspector - Installation & Usage Guide

## 📱 Installation

### Prerequisites
- Android device running Android 7.0 (API 24) or higher
- At least 50MB of free storage space
- Access to device storage (will be requested during first run)

### Step 1: Download the APK
1. Download the latest `GameFileInspector.apk` from the releases page
2. Transfer the APK to your Android device if downloaded on a computer

### Step 2: Enable Unknown Sources
1. Go to **Settings** > **Security** (or **Privacy**)
2. Enable **Unknown Sources** or **Install unknown apps**
3. For Android 8.0+: Enable installation for your file manager or browser

### Step 3: Install the Application
1. Open your file manager and navigate to the downloaded APK
2. Tap on `GameFileInspector.apk`
3. Follow the installation prompts
4. Tap **Install** when prompted

### Step 4: Grant Permissions
1. Launch the app after installation
2. Grant **Storage** permissions when prompted
3. For Android 11+: Grant **All Files Access** permission in settings

## 🎮 First Time Setup

### Initial Launch
1. Open **Game File Inspector** from your app drawer
2. The app will scan for installed games automatically
3. This may take a few moments depending on the number of apps installed

### Understanding the Interface
- **Main Screen**: Shows scan progress and quick actions
- **Game List**: Displays detected games with file access status
- **File Analysis**: Shows detected values in selected files
- **Hex Editor**: Advanced binary file editing

## 📖 Usage Guide

### Basic Workflow

#### 1. Scan for Games
```
Main Screen → Tap "Scan Games" → Wait for completion
```
- The app automatically detects installed games
- Games are categorized by data accessibility
- Green indicators show games with modifiable files

#### 2. Select a Game
```
Game List → Tap on desired game → View accessible files
```
- Browse through detected game files
- File types are indicated by icons
- Confidence ratings show likelihood of containing game values

#### 3. Analyze Files
```
File List → Tap on file → View detected values
```
- The app analyzes file content automatically
- Detected values are shown with confidence scores
- Categories help identify value types (currency, levels, etc.)

#### 4. Modify Values
```
Value List → Tap on value → Enter new value → Confirm
```
- Original files are automatically backed up
- Type validation prevents invalid entries
- Changes are applied immediately

### Advanced Features

#### Hex Editor
```
File Analysis → Menu → Hex Editor
```
- View and edit binary files directly
- Search for specific byte patterns
- Make precise modifications to file structure

#### Backup Management
```
File Analysis → Menu → Backup/Restore
```
- Create manual backups before major changes
- Restore from any previous backup
- Automatic cleanup of old backups

#### Export/Import Modifications
```
File Analysis → Menu → Export Modifications
```
- Save modification profiles for sharing
- Import profiles from other users
- Create templates for common modifications

## 🔍 File Type Support

### JSON Files (.json)
- **Best Support**: Full parsing and modification
- **Common in**: Save files, configuration files
- **Detects**: Currency, levels, progress, settings

### Properties Files (.properties, .ini, .cfg)
- **Good Support**: Key-value pair modification
- **Common in**: Game settings, user preferences
- **Detects**: Configuration values, game options

### Binary Files (.dat, .bin, .sav)
- **Limited Support**: Pattern-based detection
- **Common in**: Compressed save files, encrypted data
- **Detects**: Numeric values, simple patterns

### Database Files (.db, .sqlite)
- **Moderate Support**: Table and column analysis
- **Common in**: Complex game data storage
- **Detects**: Player data, game statistics

## 🎯 Tips for Success

### Finding Game Values

#### Currency (Gold, Coins, Gems)
1. Note your current currency amount in-game
2. Analyze save files immediately after checking
3. Look for exact matches or nearby values
4. Test modifications with small amounts first

#### Experience Points
1. Check XP before and after gaining experience
2. Look for values that match the difference
3. Experience is often stored as total, not level

#### Level/Progress
1. Level values are usually small integers (1-100)
2. Progress might be stored as percentages (0-100)
3. Some games store level as experience thresholds

### Best Practices

#### Safety First
- **Always backup** before making changes
- **Test with small values** before large modifications
- **Keep original values** noted somewhere safe
- **Restore immediately** if game becomes unstable

#### Effective Modification
- **Make realistic changes** to avoid detection
- **Modify one value at a time** for testing
- **Restart the game** after modifications
- **Check if changes persist** after game restart

#### Troubleshooting
- **Game crashes**: Restore from backup immediately
- **Values reset**: Game may have server validation
- **Can't find files**: Check if game stores data externally
- **Permission denied**: Ensure storage permissions are granted

## 🚫 Limitations & Warnings

### Technical Limitations
- **Root access not available**: Cannot modify system-protected files
- **Encrypted files**: Cannot modify encrypted or obfuscated data
- **Server-side validation**: Online games may reset modified values
- **Anti-cheat systems**: Some games detect file modifications

### Legal and Ethical Considerations
- **Terms of Service**: Modifying games may violate ToS
- **Online games**: May result in account suspension
- **Multiplayer games**: Can affect other players unfairly
- **Educational use**: Recommended for learning purposes only

### Compatibility Issues
- **Game updates**: May change file formats or locations
- **Android versions**: Newer versions have stricter file access
- **Device variations**: Some manufacturers restrict file access
- **Storage types**: External storage may have different permissions

## 🔧 Troubleshooting

### Common Issues

#### "No games found"
- Ensure storage permissions are granted
- Check if games store data in accessible locations
- Try manual file browsing in device storage

#### "Permission denied"
- Grant all storage permissions in app settings
- For Android 11+: Enable "All files access"
- Restart the app after granting permissions

#### "File modification failed"
- Check if file is read-only
- Ensure sufficient storage space
- Verify file is not currently in use by game

#### "Values not changing in game"
- Restart the game completely
- Check if game validates data on startup
- Verify correct file was modified

### Getting Help
1. Check the FAQ section in the app
2. Review this guide for missed steps
3. Create an issue on the project repository
4. Include device info and error messages

## 📚 Advanced Usage

### Custom Patterns
- Create custom detection patterns for specific games
- Export and share successful modification profiles
- Build libraries of working modifications

### Batch Operations
- Modify multiple values simultaneously
- Apply saved profiles to new game installations
- Automate common modification tasks

### File Analysis
- Compare original and modified files
- Track changes over time
- Understand game data structures

## 🔄 Updates and Maintenance

### Keeping Updated
- Check for app updates regularly
- Update may include new game support
- Backup modification profiles before updating

### Data Management
- Regularly clean up old backups
- Export important modification profiles
- Monitor storage usage

---

## ⚠️ Important Disclaimer

This application is provided for educational and research purposes only. Users are solely responsible for:

- Complying with game terms of service
- Understanding legal implications in their jurisdiction
- Any consequences of modifying game files
- Backing up important data before modifications

The developers are not responsible for:
- Game crashes or data loss
- Account suspensions or bans
- Violation of terms of service
- Any damages resulting from app usage

**Use at your own risk and always backup your data!**