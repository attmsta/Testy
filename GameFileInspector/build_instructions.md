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

This project uses **GitLab CI/CD** for Continuous Integration and Continuous Deployment (CI/CD).
The entire pipeline configuration for the Android application, including build, test, security scanning (like secret detection), and release processes, is defined in the `GameFileInspector/.gitlab-ci.yml` file.

Key features of this CI/CD setup include:
- Automated builds triggered on pushes to specific branches and tags.
- Execution of various tests (unit tests, lint checks, security scans).
- Compilation of debug and release APKs.
- Secure storage of build artifacts (APKs, test reports) within GitLab.
- Automated creation of GitLab Releases, with APKs attached.

You can view the status and logs of CI/CD pipeline runs under the "CI/CD" > "Pipelines" section of the GitLab repository.

### GitLab CI/CD Configuration Snippet
A conceptual example of a job definition within `GameFileInspector/.gitlab-ci.yml` might look like this:

```yaml
build_debug: # Job name
  stage: build # Assigns job to the 'build' stage
  script: # Commands to execute
    - echo "Building debug APK..."
    - (cd $PROJECT_SUBDIR && ./gradlew assembleDebug) # Actual command from the CI file
  artifacts: # Defines files to save after job completion
    paths:
      - $PROJECT_SUBDIR/app/build/outputs/apk/debug/app-debug.apk # Path to the artifact
    expire_in: 1 week # How long to keep the artifact
  only: # Conditions for running the job
    - branches # e.g., run for all branches
```
For the complete and actual configuration, please refer to the `GameFileInspector/.gitlab-ci.yml` file.

# The following 'jobs: build: runs-on: ubuntu-latest' was a GitHub Actions example,
# it's removed as it's not relevant to GitLab CI.
# jobs:
#   build:
#     runs-on: ubuntu-latest
    # steps:
    # - uses: actions/checkout@v3
    # - name: Set up JDK 17
    #   uses: actions/setup-java@v3
    #   with:
    #     java-version: '17'
    #     distribution: 'temurin'
    # # - name: Setup Android SDK # This is handled by image and before_script in GitLab CI
    # # - name: Build APK
    # #   run: ./gradlew assembleDebug # Example script line
    # # - name: Upload APK # Handled by artifacts:paths in GitLab CI
    # #   uses: actions/upload-artifact@v3
    # #   with:
    # #     name: app-debug
    # #     path: app/build/outputs/apk/debug/app-debug.apk
```

## Distribution

### APK Distribution
1. Download from GitLab Releases.
2. Download from GitLab CI/CD pipeline artifacts.
3. Host on your own server.
4. Distribute via email or messaging.

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