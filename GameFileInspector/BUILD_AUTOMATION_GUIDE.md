# 🚀 Build Automation Guide

## Overview

This guide covers the comprehensive build automation system for Game File Inspector, including CI/CD pipelines, local build scripts, and release management.

## 🔄 Automated Build Workflows

This project uses **GitHub Actions** for CI/CD. The workflow is defined in `.github/workflows/build-and-release.yml`.

### GitHub Actions Workflow (`.github/workflows/build-and-release.yml`)

The GitHub Actions workflow provides comprehensive automated building, testing, and releasing, deeply integrated with GitHub.

#### Workflow Jobs
The workflow consists of several key jobs:
1.  **`test`**: Runs unit tests and lint checks on the codebase. This ensures code quality and catches regressions early.
2.  **`build-debug`**: Compiles a debug version of the APK. This is typically run on pull requests and pushes to non-main branches to verify build integrity.
3.  **`build-release`**: Compiles a release version of the APK (unsigned). This is run on pushes to the `main` branch and when tags are created.
4.  **`create-release`**: Automatically creates a GitHub Release when changes are pushed to the `main` branch or a tag is created. This job downloads the release APK from `build-release` and attaches it to the GitHub Release, along with generated release notes.

*(Note: The workflow may include additional jobs for security scanning, performance analysis, etc., as configured in the YAML file.)*

#### Key Features & Advanced Capabilities
- ✅ **Automatic APK Building**: Triggered on pushes to `main`, `develop`, tags (`v*`), pull requests to `main`, scheduled nightly builds, and manual dispatches.
- ✅ **Comprehensive Testing**: Includes unit tests and lint analysis.
- ✅ **Artifact Management**: APKs and test results are stored as artifacts with configurable retention.
- ✅ **Automated GitHub Releases**: New releases are automatically drafted or published with APKs and detailed release notes.
- ✅ **Security Scanning**: (If configured) Checks for hardcoded secrets, permission issues, etc.
- ✅ **Performance Analysis**: (If configured) Monitors APK size and other performance metrics.
- 🎯 **Smart Triggering**: Builds and releases are triggered based on branch names (e.g., `main`, `develop`), event types (push, pull_request, tag), and file changes.
- 🎯 **Artifact Caching**: Utilizes caching for Gradle dependencies (`~/.gradle/caches`, `~/.gradle/wrapper`, `GameFileInspector/.gradle`) to speed up build times. The cache key is based on OS and hash of Gradle files.
- 🎯 **Multi-job Parallelization**: Jobs can run in parallel where appropriate to improve CI/CD efficiency.
- 🎯 **Conditional Execution**: Jobs and steps can be run conditionally (e.g., `build-release` runs on `main` or tags, `create-release` depends on specific conditions).
- 🎯 **Release Management**: Automatic versioning and release note generation are part of the `create-release` job.

#### Accessing Build Artifacts and Logs
- **Build Logs**: Available directly within the "Actions" tab of the GitHub repository. Each workflow run shows detailed logs for every step.
- **APKs**:
    - Debug APKs are uploaded as artifacts in the `build-debug` job runs.
    - Release APKs are uploaded as artifacts in the `build-release` job runs.
    - Release APKs are also attached to GitHub Releases created by the `create-release` job.
- **Test Reports**: Uploaded as artifacts in the `test` job runs.

## 🛠️ Local Build Script (`build_apk.sh`)

### Features
The local build script provides comprehensive APK building capabilities:

```bash
# Basic usage
./build_apk.sh                    # Build debug APK
./build_apk.sh -t release         # Build release APK
./build_apk.sh -c -t release      # Clean build release APK
./build_apk.sh -t debug -i        # Build and install debug APK
```

### Command Line Options
| Option | Description | Example |
|--------|-------------|---------|
| `-t, --type` | Build type (debug/release) | `-t release` |
| `-c, --clean` | Clean build artifacts | `-c` |
| `--no-tests` | Skip running tests | `--no-tests` |
| `-i, --install` | Install APK after building | `-i` |
| `-d, --device` | Show connected devices | `-d` |
| `-h, --help` | Show help message | `-h` |

### Build Process
1. **Environment Validation** - Check Java, Android SDK, Gradle
2. **Dependency Resolution** - Download and cache dependencies
3. **Testing** (optional) - Run unit tests and lint checks
4. **Compilation** - Build APK with specified configuration
5. **Analysis** - APK size analysis and optimization suggestions
6. **Installation** (optional) - Install on connected devices
7. **Reporting** - Generate detailed build report

### Build Report Generation
The script generates comprehensive build reports:
```
Game File Inspector - Build Report
==================================
Generated: 2025-06-15 14:30:00

Build Configuration:
- Build Type: release
- Clean Build: true
- Tests Run: true
- Build Duration: 45s

APK Information:
- File: app/build/outputs/apk/release/app-release-unsigned.apk
- Size: 12.5MB
- Target SDK: 34 (Android 14)
- Minimum SDK: 24 (Android 7.0)

Environment:
- Java: openjdk version "17.0.2"
- Gradle: Gradle 8.2
- Android SDK: /home/user/Android/Sdk
- OS: Linux 5.15.0

Build Status: SUCCESS ✅
```

## 📦 Release Management

### Automatic Release Creation

#### GitHub Releases
- **Trigger**: Configured within the GitHub Actions workflow (`.github/workflows/build-and-release.yml`). Typically on pushes to the `main` branch or when new tags (e.g., `v1.0.0`) are pushed. Can also be triggered manually via `workflow_dispatch`.
- **Assets**: The workflow uploads the release APK (e.g., `app-release-unsigned.apk`) and a versioned copy (e.g., `GameFileInspector-v<SHA>.apk`) to the GitHub Release.
- **Versioning**: The `create-release` job in the workflow generates a version name. If triggered by a tag, it uses the tag name. Otherwise, it might use a date-based version with a commit SHA.
- **Release Notes**: Automatically generated as part of the `create-release` job, summarizing features and build information.
- **Access**: Releases are available under the "Releases" section of the GitHub repository.

### Release Artifacts
Each release includes:
- 📱 **Release APK** - Optimized production build
- 📱 **Debug APK** - Development build with debugging enabled
- 📊 **Build Reports** - Comprehensive build and test results
- 📝 **Release Notes** - Feature descriptions and change logs
- 🔍 **Security Reports** - Security analysis results

## 🔧 Build Configuration

### Gradle Configuration
The build system uses Gradle 8.2 with optimized settings:

```gradle
android {
    compileSdk 34
    
    defaultConfig {
        applicationId "com.gamefileinspector"
        minSdk 24
        targetSdk 34
        versionCode 1
        versionName "1.0"
    }
    
    buildTypes {
        debug {
            debuggable true
            minifyEnabled false
            applicationIdSuffix ".debug"
        }
        
        release {
            minifyEnabled true
            proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'
            signingConfig signingConfigs.debug // For unsigned builds
        }
    }
}
```

### Build Optimization
- **ProGuard/R8** - Code shrinking and obfuscation for release builds
- **Resource Optimization** - Automatic resource shrinking
- **APK Splitting** - Architecture-specific APKs (optional)
- **Bundle Generation** - Android App Bundle support

## 🧪 Testing Integration

### Automated Testing
- **Unit Tests** - Comprehensive test suite with Robolectric
- **Lint Analysis** - Code quality and best practice checks
- **Security Scanning** - Vulnerability and secret detection
- **Performance Testing** - APK size and method count analysis

### Test Reporting
- **JUnit Reports** - Standard test result format
- **Coverage Reports** - Code coverage analysis
- **Lint Reports** - Detailed code quality metrics
- **Security Reports** - Security vulnerability assessments

## 🔒 Security and Signing

### Security Measures
- **Secret Scanning** - Automated detection of hardcoded secrets
- **Permission Analysis** - Review of requested Android permissions
- **Dependency Scanning** - Analysis of third-party dependencies
- **Code Quality Checks** - Static analysis for security issues

### APK Signing
For production releases, configure signing:

```bash
# Generate keystore (one-time setup)
keytool -genkey -v -keystore release-key.keystore -alias release -keyalg RSA -keysize 2048 -validity 10000

# Configure signing in build.gradle
android {
    signingConfigs {
        release {
            storeFile file('release-key.keystore')
            storePassword System.getenv('KEYSTORE_PASSWORD')
            keyAlias 'release'
            keyPassword System.getenv('KEY_PASSWORD')
        }
    }
}
```

## 📊 Performance Monitoring

### Build Performance
- **Build Time Tracking** - Monitor compilation duration
- **Cache Efficiency** - Gradle and dependency cache utilization
- **Resource Usage** - Memory and CPU usage during builds
- **Parallel Execution** - Multi-core build optimization

### APK Analysis
- **Size Monitoring** - Track APK size growth over time
- **Method Count** - Monitor DEX method count limits
- **Resource Analysis** - Identify large resources and assets
- **Architecture Support** - Multi-architecture build analysis

## 🚀 Deployment Strategies

### Development Deployment
1. **Feature Branches** - Debug builds for testing
2. **Pull Requests** - Automated builds for code review
3. **Development Releases** - Internal testing builds

### Production Deployment
1. **Release Candidates** - Pre-release testing builds
2. **Stable Releases** - Production-ready builds
3. **Hotfix Releases** - Critical bug fix deployments

### Distribution Channels
- **GitHub Releases** - Public distribution with download links
- **Direct APK** - Manual installation for testing (can be downloaded from GitHub Actions artifacts or releases)
- **App Stores** - Future Google Play Store distribution

## 🔄 Continuous Integration Best Practices

### Branch Strategy
```
main branch:
├── Automatic release builds
├── Comprehensive testing
├── Security scanning
└── Production deployment

feature branches:
├── Debug builds only
├── Basic testing
├── Pull request validation
└── Development feedback

release tags:
├── Stable release builds
├── Full test suite
├── Security validation
└── Public distribution
```

### Build Triggers
- **Push Events** - Automatic builds on code changes
- **Pull Requests** - Validation builds for code review
- **Scheduled Builds** - Nightly builds for continuous testing
- **Manual Triggers** - On-demand builds for specific needs

### Artifact Management
- **Retention Policies** - Automatic cleanup of old artifacts
- **Storage Optimization** - Efficient artifact storage
- **Access Control** - Secure artifact distribution
- **Version Tracking** - Comprehensive build history

## 🛠️ Troubleshooting

### Common Build Issues

#### Android SDK Not Found
```bash
# Set environment variables
export ANDROID_HOME=/path/to/android-sdk
export ANDROID_SDK_ROOT=/path/to/android-sdk
export PATH=$PATH:$ANDROID_HOME/platform-tools
```

#### Gradle Build Failures
```bash
# Clean and rebuild
./gradlew clean
./gradlew assembleDebug --stacktrace --info

# Check Gradle wrapper
./gradlew wrapper --gradle-version 8.2
```

#### Memory Issues
```bash
# Increase Gradle memory
export GRADLE_OPTS="-Xmx4g -XX:MaxMetaspaceSize=512m"

# Enable parallel builds
echo "org.gradle.parallel=true" >> gradle.properties
```

#### Dependency Resolution
```bash
# Refresh dependencies
./gradlew --refresh-dependencies

# Clear Gradle cache
rm -rf ~/.gradle/caches/
```

### CI/CD Troubleshooting

#### Pipeline Failures
1. **Check Logs** - Review detailed build logs
2. **Environment Issues** - Verify SDK and tool versions
3. **Dependency Problems** - Check for version conflicts
4. **Resource Limits** - Monitor memory and disk usage

#### Artifact Issues
1. **Upload Failures** - Check artifact size limits
2. **Download Problems** - Verify artifact retention policies
3. **Permission Issues** - Review access control settings
4. **Storage Limits** - Monitor repository storage usage

## 📈 Metrics and Monitoring

### Build Metrics
- **Success Rate** - Percentage of successful builds
- **Build Duration** - Average and trend analysis
- **Test Coverage** - Code coverage percentage
- **APK Size** - Size growth tracking

### Quality Metrics
- **Lint Issues** - Code quality trend analysis
- **Security Vulnerabilities** - Security issue tracking
- **Performance Regressions** - Performance impact monitoring
- **Dependency Updates** - Outdated dependency tracking

## 🎯 Future Enhancements

### Planned Improvements
- **Signed APK Builds** - Production signing integration
- **App Bundle Support** - Android App Bundle generation
- **Multi-flavor Builds** - Different app variants
- **Automated Testing** - UI and integration tests
- **Performance Profiling** - Detailed performance analysis
- **Store Deployment** - Google Play Store integration

### Advanced Features
- **A/B Testing** - Multiple build variants
- **Feature Flags** - Runtime feature toggling
- **Crash Reporting** - Automated crash analysis
- **Analytics Integration** - Usage and performance metrics
- **Update Mechanisms** - In-app update capabilities

---

## 📚 Quick Reference

### Essential Commands
```bash
# Local development
./build_apk.sh -t debug -i        # Build and install debug APK
./build_apk.sh -c -t release      # Clean release build
./run_tests.sh                    # Run comprehensive tests

# CI/CD triggers
git push origin main              # Trigger release build
git tag v1.0.0 && git push --tags # Trigger tagged release
```

### Important Files
- `.github/workflows/build-and-release.yml` - GitHub Actions workflow configuration.
- `build_apk.sh` - Local build script for development and manual builds.
- `run_tests.sh` - Test execution script
- `gradle.properties` - Gradle build configuration

### Support Resources
- **Documentation** - Complete guides in repository
- **Issue Tracking** - GitHub Issues for bug reports and feature requests.
- **Build Logs** - Detailed execution logs available in the GitHub Actions tab for each workflow run.
- **Community** - Developer forums and discussions (if applicable).

This automated build system, powered by GitHub Actions, ensures reliable, consistent, and secure APK generation for every code change, enabling rapid development and deployment of the Game File Inspector application.