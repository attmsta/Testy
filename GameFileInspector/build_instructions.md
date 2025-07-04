# Build Instructions for Game File Inspector

## Prerequisites

### Required Software
1. **Android Studio** (Arctic Fox or later)
   - Download from: https://developer.android.com/studio
   - Includes Android SDK and build tools

2. **Java Development Kit (JDK) 17**
   - OpenJDK 17 or Oracle JDK 17
   - Set JAVA_HOME environment variable

3. **Android SDK**
   - API Level 24 (Android 7.0) minimum
   - API Level 34 (Android 14) target
   - Android SDK Build-Tools 34.0.0

### Environment Setup

#### Windows
```cmd
set JAVA_HOME=C:\Program Files\Java\jdk-17
set ANDROID_HOME=C:\Users\%USERNAME%\AppData\Local\Android\Sdk
set PATH=%PATH%;%ANDROID_HOME%\tools;%ANDROID_HOME%\platform-tools
```

#### macOS/Linux
```bash
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk
export ANDROID_HOME=$HOME/Android/Sdk
export PATH=$PATH:$ANDROID_HOME/tools:$ANDROID_HOME/platform-tools
```

## Building the Project

### Method 1: Android Studio (Recommended)

1. **Open Project**
   ```
   File → Open → Select GameFileInspector folder
   ```

2. **Sync Project**
   ```
   Tools → Android → Sync Project with Gradle Files
   ```

3. **Build APK**
   ```
   Build → Build Bundle(s) / APK(s) → Build APK(s)
   ```

4. **Locate APK**
   ```
   app/build/outputs/apk/debug/app-debug.apk
   ```

### Method 2: Command Line

1. **Navigate to Project Directory**
   ```bash
   cd GameFileInspector
   ```

2. **Make Gradlew Executable** (Linux/macOS)
   ```bash
   chmod +x gradlew
   ```

3. **Build Debug APK**
   ```bash
   ./gradlew assembleDebug
   ```

4. **Build Release APK**
   ```bash
   ./gradlew assembleRelease
   ```

### Method 3: Docker Build (Advanced)

1. **Create Dockerfile**
   ```dockerfile
   FROM openjdk:17-jdk-slim
   
   # Install Android SDK
   RUN apt-get update && apt-get install -y wget unzip
   RUN wget https://dl.google.com/android/repository/commandlinetools-linux-9477386_latest.zip
   RUN unzip commandlinetools-linux-9477386_latest.zip
   
   ENV ANDROID_HOME=/opt/android-sdk
   ENV PATH=$PATH:$ANDROID_HOME/cmdline-tools/bin:$ANDROID_HOME/platform-tools
   
   # Accept licenses and install SDK components
   RUN yes | sdkmanager --sdk_root=$ANDROID_HOME --licenses
   RUN sdkmanager --sdk_root=$ANDROID_HOME "platform-tools" "platforms;android-34" "build-tools;34.0.0"
   
   WORKDIR /app
   COPY . .
   
   RUN ./gradlew assembleDebug
   ```

2. **Build with Docker**
   ```bash
   docker build -t gamefileinspector-build .
   docker run --rm -v $(pwd)/app/build/outputs:/output gamefileinspector-build
   ```

## Build Variants

### Debug Build
- **Purpose**: Development and testing
- **Features**: Debugging enabled, logging verbose
- **Command**: `./gradlew assembleDebug`
- **Output**: `app-debug.apk`

### Release Build
- **Purpose**: Production distribution
- **Features**: Optimized, obfuscated, signed
- **Command**: `./gradlew assembleRelease`
- **Output**: `app-release.apk`

### Signing Release APK

1. **Generate Keystore**
   ```bash
   keytool -genkey -v -keystore release-key.keystore -alias gamefileinspector -keyalg RSA -keysize 2048 -validity 10000
   ```

2. **Configure Signing in build.gradle**
   ```gradle
   android {
       signingConfigs {
           release {
               storeFile file('release-key.keystore')
               storePassword 'your_store_password'
               keyAlias 'gamefileinspector'
               keyPassword 'your_key_password'
           }
       }
       buildTypes {
           release {
               signingConfig signingConfigs.release
               minifyEnabled true
               proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'
           }
       }
   }
   ```

## Troubleshooting Build Issues

### Common Problems

#### "SDK location not found"
**Solution**: Set ANDROID_HOME environment variable
```bash
export ANDROID_HOME=/path/to/android/sdk
```

#### "Gradle version incompatible"
**Solution**: Update gradle wrapper
```bash
./gradlew wrapper --gradle-version 8.2
```

#### "Build tools not found"
**Solution**: Install required build tools
```bash
sdkmanager "build-tools;34.0.0"
```

#### "Java version incompatible"
**Solution**: Use Java 17
```bash
export JAVA_HOME=/path/to/java-17
```

### Clean Build
```bash
./gradlew clean
./gradlew assembleDebug
```

### Verbose Build Output
```bash
./gradlew assembleDebug --info --stacktrace
```

## Testing the Build

### Install on Device
```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

### Install on Emulator
```bash
adb -e install app/build/outputs/apk/debug/app-debug.apk
```

### Run Tests
```bash
./gradlew test
./gradlew connectedAndroidTest
```

## Optimization

### ProGuard Configuration
Create `proguard-rules.pro`:
```proguard
# Keep application class
-keep class com.gamefileinspector.** { *; }

# Keep model classes
-keep class com.gamefileinspector.models.** { *; }

# Keep JSON parsing
-keepattributes Signature
-keepattributes *Annotation*
-keep class org.json.** { *; }
```

### Build Performance
```gradle
# In gradle.properties
org.gradle.jvmargs=-Xmx4g -XX:MaxMetaspaceSize=512m
org.gradle.parallel=true
org.gradle.caching=true
android.useAndroidX=true
android.enableJetifier=true
```

## Continuous Integration

This project utilizes a dual CI/CD setup:

1.  **GitHub Actions** (`GameFileInspector/.github/workflows/android-release-apk.yml`):
    *   **Primary role**: Building unsigned release APKs and creating official GitHub Releases when version tags (e.g., `v1.0.0`) are pushed.
    *   **Triggers**: Pushes to `main`, version tags (`v*.*.*`), and manual dispatch.
    *   **Artifacts**: Unsigned release APKs are available as artifacts in workflow runs (under the "Actions" tab on GitHub) and are attached to GitHub Releases.
    *   **Key Jobs**: `build-release-apk` (builds APK) and `create-github-release` (creates GitHub Release).

2.  **GitLab CI/CD** (`GameFileInspector/.gitlab-ci.yml`):
    *   **Role**: Handles general continuous integration tasks such as running tests on pushes/merge requests, building debug APKs, nightly builds, and integrated security scans (e.g., secret detection).
    *   **Triggers**: Pushes to branches, merge requests, scheduled tasks.
    *   **Artifacts**: Debug APKs, test reports, and other build outputs are available from GitLab pipeline jobs. GitLab Releases might be used for internal previews.
    *   **Key Stages**: `prepare`, `test`, `secret-detection`, `build`, `release`.

For official release APKs, refer to the **GitHub Releases** page, populated by the GitHub Actions workflow. For other CI checks, debug builds, or specific test reports, refer to the GitLab CI/CD pipelines.

### Conceptual CI Snippet (GitHub Actions - Release Build)
A simplified view of a build step in `GameFileInspector/.github/workflows/android-release-apk.yml`:
```yaml
# Example from build-release-apk job in GitHub Actions
# ...
    - name: 🚀 Build unsigned release APK
      run: ./gradlew assembleRelease --stacktrace
      working-directory: ${{ env.ANDROID_PROJECT_PATH }} # Which is GameFileInspector

    - name: 📤 Upload APK Artifact
      uses: actions/upload-artifact@v4
      with:
        name: release-apk
        path: ${{ env.ANDROID_PROJECT_PATH }}/app/build/outputs/apk/release/GameFileInspector-release-unsigned.apk
# ...
```
For full details, consult the respective workflow YAML files.

## Distribution

### APK Distribution
1.  **Official Releases**: Download from the **GitHub Releases** page for the repository. These are generated by the GitHub Actions workflow when version tags are pushed.
2.  **Development/Debug Builds**: Can be downloaded from GitLab CI/CD pipeline artifacts for testing or internal use.
3.  **Local Builds**: You can build directly from source for development (see "Building the Project" above).
4.  Other methods like hosting on your own server or direct distribution are also possible.

### Play Store (Future)
1. Create developer account
2. Prepare store listing
3. Upload signed APK
4. Complete review process

## Version Management

### Update Version
In `app/build.gradle`:
```gradle
android {
    defaultConfig {
        versionCode 2
        versionName "1.1.0"
    }
}
```

### Tag Releases
```bash
git tag -a v1.1.0 -m "Version 1.1.0"
git push origin v1.1.0
```

---

## Quick Build Commands

### Development
```bash
# Clean and build debug
./gradlew clean assembleDebug

# Install on connected device
./gradlew installDebug

# Run tests
./gradlew test
```

### Release
```bash
# Build signed release
./gradlew assembleRelease

# Generate bundle for Play Store
./gradlew bundleRelease
```

### Maintenance
```bash
# Update dependencies
./gradlew dependencyUpdates

# Check for security issues
./gradlew dependencyCheckAnalyze

# Generate documentation
./gradlew dokkaHtml
```