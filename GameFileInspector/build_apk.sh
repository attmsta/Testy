#!/bin/bash

# Game File Inspector - Local APK Build Script
# This script builds APK files locally for development and testing

set -e  # Exit on any error

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
PURPLE='\033[0;35m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

# Function to print colored output
print_header() {
    echo -e "${PURPLE}================================${NC}"
    echo -e "${PURPLE}$1${NC}"
    echo -e "${PURPLE}================================${NC}"
    echo ""
}

print_status() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

print_step() {
    echo -e "${CYAN}➤${NC} $1"
}

# Default values
BUILD_TYPE="debug"
CLEAN_BUILD=false
RUN_TESTS=true
INSTALL_APK=false
DEVICE_INSTALL=false
SHOW_HELP=false

# Parse command line arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        -t|--type)
            BUILD_TYPE="$2"
            shift 2
            ;;
        -c|--clean)
            CLEAN_BUILD=true
            shift
            ;;
        --no-tests)
            RUN_TESTS=false
            shift
            ;;
        -i|--install)
            INSTALL_APK=true
            shift
            ;;
        -d|--device)
            DEVICE_INSTALL=true
            shift
            ;;
        -h|--help)
            SHOW_HELP=true
            shift
            ;;
        *)
            print_error "Unknown option: $1"
            SHOW_HELP=true
            shift
            ;;
    esac
done

# Show help
if [ "$SHOW_HELP" = true ]; then
    echo "Game File Inspector - APK Build Script"
    echo ""
    echo "Usage: $0 [OPTIONS]"
    echo ""
    echo "Options:"
    echo "  -t, --type TYPE     Build type: debug, release (default: debug)"
    echo "  -c, --clean         Clean build (remove previous build artifacts)"
    echo "  --no-tests          Skip running tests"
    echo "  -i, --install       Install APK after building (requires ADB)"
    echo "  -d, --device        Show connected devices"
    echo "  -h, --help          Show this help message"
    echo ""
    echo "Examples:"
    echo "  $0                          # Build debug APK"
    echo "  $0 -t release              # Build release APK"
    echo "  $0 -c -t release           # Clean build release APK"
    echo "  $0 -t debug -i             # Build debug APK and install"
    echo "  $0 --no-tests -t release   # Build release APK without tests"
    echo ""
    exit 0
fi

# Validate build type
if [[ "$BUILD_TYPE" != "debug" && "$BUILD_TYPE" != "release" ]]; then
    print_error "Invalid build type: $BUILD_TYPE. Must be 'debug' or 'release'"
    exit 1
fi

print_header "🎮 Game File Inspector APK Builder"

# Check if we're in the right directory
if [ ! -f "app/build.gradle" ]; then
    print_error "Please run this script from the GameFileInspector root directory"
    exit 1
fi

# Check if gradlew exists
if [ ! -f "gradlew" ]; then
    print_error "gradlew not found. Please ensure you're in the correct directory."
    exit 1
fi

# Make gradlew executable
chmod +x gradlew

# Show build configuration
print_status "Build Configuration:"
echo "  • Build Type: $BUILD_TYPE"
echo "  • Clean Build: $CLEAN_BUILD"
echo "  • Run Tests: $RUN_TESTS"
echo "  • Install APK: $INSTALL_APK"
echo ""

# Check for Android SDK
if [ -z "$ANDROID_HOME" ] && [ -z "$ANDROID_SDK_ROOT" ]; then
    print_warning "ANDROID_HOME or ANDROID_SDK_ROOT not set"
    print_status "Attempting to detect Android SDK..."
    
    # Common Android SDK locations
    POSSIBLE_SDK_PATHS=(
        "$HOME/Android/Sdk"
        "$HOME/Library/Android/sdk"
        "/opt/android-sdk"
        "/usr/local/android-sdk"
    )
    
    for path in "${POSSIBLE_SDK_PATHS[@]}"; do
        if [ -d "$path" ]; then
            export ANDROID_HOME="$path"
            export ANDROID_SDK_ROOT="$path"
            print_success "Found Android SDK at: $path"
            break
        fi
    done
    
    if [ -z "$ANDROID_HOME" ]; then
        print_warning "Android SDK not found. Build may fail."
        print_status "Please install Android SDK and set ANDROID_HOME environment variable"
    fi
fi

# Check Java version
print_step "Checking Java version..."
if command -v java >/dev/null 2>&1; then
    JAVA_VERSION=$(java -version 2>&1 | head -1 | cut -d'"' -f2 | cut -d'.' -f1)
    if [ "$JAVA_VERSION" -ge 17 ]; then
        print_success "Java $JAVA_VERSION detected"
    else
        print_warning "Java $JAVA_VERSION detected. Java 17+ recommended for Android development"
    fi
else
    print_error "Java not found. Please install Java 17+ and add it to PATH"
    exit 1
fi

# Check Gradle version
print_step "Checking Gradle version..."
GRADLE_VERSION=$(./gradlew --version | grep "Gradle" | head -1)
print_success "$GRADLE_VERSION"

# Clean build if requested
if [ "$CLEAN_BUILD" = true ]; then
    print_step "Cleaning previous build artifacts..."
    ./gradlew clean
    print_success "Clean completed"
fi

# Run tests if requested
if [ "$RUN_TESTS" = true ]; then
    print_step "Running unit tests..."
    if ./gradlew test --stacktrace; then
        print_success "Unit tests passed"
    else
        print_warning "Some unit tests failed. Check the reports for details."
        read -p "Continue with build? (y/n): " -n 1 -r
        echo
        if [[ ! $REPLY =~ ^[Yy]$ ]]; then
            print_error "Build cancelled by user"
            exit 1
        fi
    fi
    
    print_step "Running lint checks..."
    if ./gradlew lint --stacktrace; then
        print_success "Lint checks passed"
    else
        print_warning "Lint found issues. Check the reports for details."
    fi
fi

# Build APK
print_step "Building $BUILD_TYPE APK..."
BUILD_START_TIME=$(date +%s)

if [ "$BUILD_TYPE" = "debug" ]; then
    if ./gradlew assembleDebug --stacktrace; then
        APK_PATH="app/build/outputs/apk/debug/app-debug.apk"
        print_success "Debug APK built successfully"
    else
        print_error "Debug APK build failed"
        exit 1
    fi
else
    if ./gradlew assembleRelease --stacktrace; then
        APK_PATH="app/build/outputs/apk/release/app-release-unsigned.apk"
        print_success "Release APK built successfully"
    else
        print_error "Release APK build failed"
        exit 1
    fi
fi

BUILD_END_TIME=$(date +%s)
BUILD_DURATION=$((BUILD_END_TIME - BUILD_START_TIME))

# Get APK information
if [ -f "$APK_PATH" ]; then
    APK_SIZE=$(du -h "$APK_PATH" | cut -f1)
    APK_SIZE_BYTES=$(stat -c%s "$APK_PATH" 2>/dev/null || stat -f%z "$APK_PATH" 2>/dev/null || echo "0")
    APK_SIZE_MB=$((APK_SIZE_BYTES / 1024 / 1024))
    
    print_success "APK Information:"
    echo "  • File: $APK_PATH"
    echo "  • Size: $APK_SIZE (${APK_SIZE_MB}MB)"
    echo "  • Build Time: ${BUILD_DURATION}s"
    
    # APK size analysis
    if [ $APK_SIZE_MB -gt 50 ]; then
        print_warning "APK size is large (${APK_SIZE_MB}MB) - consider optimization"
    elif [ $APK_SIZE_MB -gt 25 ]; then
        print_status "APK size is moderate (${APK_SIZE_MB}MB)"
    else
        print_success "APK size is good (${APK_SIZE_MB}MB)"
    fi
else
    print_error "APK file not found at expected location: $APK_PATH"
    exit 1
fi

# Check for connected devices if installation requested
if [ "$INSTALL_APK" = true ] || [ "$DEVICE_INSTALL" = true ]; then
    print_step "Checking for connected Android devices..."
    
    if command -v adb >/dev/null 2>&1; then
        DEVICES=$(adb devices | grep -v "List of devices" | grep "device$" | wc -l)
        
        if [ "$DEVICES" -gt 0 ]; then
            print_success "Found $DEVICES connected device(s)"
            
            if [ "$DEVICE_INSTALL" = true ]; then
                echo ""
                print_status "Connected devices:"
                adb devices | grep "device$" | while read device; do
                    DEVICE_ID=$(echo $device | cut -f1)
                    DEVICE_MODEL=$(adb -s $DEVICE_ID shell getprop ro.product.model 2>/dev/null || echo "Unknown")
                    echo "  • $DEVICE_ID ($DEVICE_MODEL)"
                done
                echo ""
            fi
            
            if [ "$INSTALL_APK" = true ]; then
                print_step "Installing APK on connected device(s)..."
                if adb install -r "$APK_PATH"; then
                    print_success "APK installed successfully"
                    
                    # Try to launch the app
                    print_step "Attempting to launch the app..."
                    if adb shell am start -n com.gamefileinspector/.MainActivity >/dev/null 2>&1; then
                        print_success "App launched successfully"
                    else
                        print_warning "Could not launch app automatically"
                    fi
                else
                    print_warning "Failed to install APK"
                fi
            fi
        else
            print_warning "No Android devices connected"
            print_status "Connect a device via USB and enable USB debugging to install APK"
        fi
    else
        print_warning "ADB not found. Cannot check for connected devices or install APK"
        print_status "Install Android SDK platform-tools to enable device installation"
    fi
fi

# Generate build report
print_step "Generating build report..."
REPORT_FILE="build_report_$(date +%Y%m%d_%H%M%S).txt"

cat > "$REPORT_FILE" << EOF
Game File Inspector - Build Report
==================================
Generated: $(date)

Build Configuration:
- Build Type: $BUILD_TYPE
- Clean Build: $CLEAN_BUILD
- Tests Run: $RUN_TESTS
- Build Duration: ${BUILD_DURATION}s

APK Information:
- File: $APK_PATH
- Size: $APK_SIZE (${APK_SIZE_MB}MB)
- Target SDK: 34 (Android 14)
- Minimum SDK: 24 (Android 7.0)

Environment:
- Java: $(java -version 2>&1 | head -1)
- Gradle: $GRADLE_VERSION
- Android SDK: ${ANDROID_HOME:-"Not set"}
- OS: $(uname -s) $(uname -r)

Build Status: SUCCESS ✅

Next Steps:
1. Test the APK on physical devices
2. Verify all features work correctly
3. Check performance on different Android versions
4. Consider running instrumented tests if available

APK Location: $APK_PATH
EOF

print_success "Build report saved: $REPORT_FILE"

# Final summary
echo ""
print_header "🎉 Build Complete!"
echo ""
print_success "Summary:"
echo "  • Build Type: $BUILD_TYPE"
echo "  • APK Size: $APK_SIZE"
echo "  • Build Time: ${BUILD_DURATION}s"
echo "  • APK Location: $APK_PATH"
echo ""

if [ "$INSTALL_APK" != true ]; then
    print_status "To install the APK manually:"
    echo "  1. Transfer $APK_PATH to your Android device"
    echo "  2. Enable 'Install from Unknown Sources' in Settings"
    echo "  3. Open the APK file to install"
    echo ""
    
    if command -v adb >/dev/null 2>&1; then
        print_status "Or install via ADB:"
        echo "  adb install -r \"$APK_PATH\""
        echo ""
    fi
fi

print_status "Build artifacts:"
echo "  • APK: $APK_PATH"
echo "  • Test Reports: app/build/reports/"
echo "  • Build Report: $REPORT_FILE"
echo ""

print_success "Ready for testing! 🚀"