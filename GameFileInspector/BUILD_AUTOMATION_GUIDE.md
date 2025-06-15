# 🚀 Build Automation Guide

## Overview

This guide covers the comprehensive build automation system for Game File Inspector, including CI/CD pipelines, local build scripts, and release management.

## 🔄 Automated Build Workflows

### GitLab CI/CD Pipeline (`.gitlab-ci.yml`)

The GitLab CI/CD pipeline provides comprehensive automated building, testing, and releasing:

#### Pipeline Stages
1. **Prepare** - Environment setup and dependency caching
2. **Test** - Unit tests and lint checks
3. **Build** - Debug and release APK compilation
4. **Release** - Automated release creation with APK artifacts

#### Key Features
- ✅ **Automatic APK Building** on every push to main branch
- ✅ **Comprehensive Testing** with unit tests and lint analysis
- ✅ **Artifact Management** with configurable retention periods
- ✅ **Release Automation** with detailed release notes
- ✅ **Security Scanning** for hardcoded secrets and permissions
- ✅ **Performance Analysis** with APK size monitoring
- ✅ **Nightly Builds** for continuous integration
- ✅ **Manual Deployment** options for controlled releases

#### Trigger Conditions
```yaml
# Automatic triggers
- Push to main branch → Release APK build
- Push to other branches → Debug APK build
- Scheduled nightly builds → Full build with reports
- Manual triggers → Configurable build types
```

### GitHub Actions Workflow (`.github/workflows/build-and-release.yml`)

The GitHub Actions workflow provides cross-platform CI/CD with GitHub integration:

#### Workflow Jobs
1. **Test** - Comprehensive testing suite
2. **Build Debug** - Debug APK for pull requests and branches
3. **Build Release** - Release APK for main branch and tags
4. **Security Scan** - Security analysis and vulnerability checks
5. **Create Release** - Automated GitHub releases with APK downloads
6. **Performance Analysis** - APK size and performance monitoring
7. **Code Quality** - Code statistics and quality metrics

#### Advanced Features
- 🎯 **Smart Triggering** based on branch and event type
- 🎯 **Artifact Caching** for faster builds
- 🎯 **Multi-job Parallelization** for efficient CI/CD
- 🎯 **Conditional Execution** based on file changes
- 🎯 **Release Management** with automatic versioning

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

#### GitLab Releases
- **Trigger**: Push to main branch or tags
- **Artifacts**: Release APK with detailed metadata
- **Versioning**: Automatic version generation based on commit SHA
- **Release Notes**: Comprehensive feature list and build information

#### GitHub Releases
- **Trigger**: Push to main branch, tags, or manual workflow dispatch
- **Assets**: Multiple APK variants with descriptive names
- **Versioning**: Date-based or tag-based version naming
- **Documentation**: Detailed installation and usage instructions

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
- **GitLab Releases** - Internal distribution with access control
- **Direct APK** - Manual installation for testing
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
- `.gitlab-ci.yml` - GitLab CI/CD pipeline configuration
- `.github/workflows/build-and-release.yml` - GitHub Actions workflow
- `build_apk.sh` - Local build script
- `run_tests.sh` - Test execution script
- `gradle.properties` - Gradle build configuration

### Support Resources
- **Documentation** - Complete guides in repository
- **Issue Tracking** - GitHub/GitLab issue systems
- **Build Logs** - Detailed CI/CD execution logs
- **Community** - Developer forums and discussions

This automated build system ensures reliable, consistent, and secure APK generation for every code change, enabling rapid development and deployment of the Game File Inspector application.