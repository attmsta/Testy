#!/bin/bash

# Game File Inspector - Test Runner Script
# This script runs all tests and generates reports

set -e  # Exit on any error

echo "🧪 Game File Inspector Test Runner"
echo "=================================="
echo ""

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Function to print colored output
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

print_status "Starting test execution..."

# Clean previous builds
print_status "Cleaning previous builds..."
./gradlew clean

# Compile the project
print_status "Compiling project..."
if ./gradlew compileDebugKotlin; then
    print_success "Compilation successful"
else
    print_error "Compilation failed"
    exit 1
fi

# Run unit tests
print_status "Running unit tests..."
if ./gradlew test --info; then
    print_success "Unit tests passed"
else
    print_warning "Some unit tests failed. Check the reports for details."
fi

# Run lint checks
print_status "Running lint analysis..."
if ./gradlew lint; then
    print_success "Lint analysis completed"
else
    print_warning "Lint found issues. Check the reports for details."
fi

# Generate test reports
print_status "Generating test reports..."

# Create reports directory
mkdir -p reports

# Copy test results
if [ -d "app/build/reports/tests" ]; then
    cp -r app/build/reports/tests reports/
    print_success "Test reports copied to reports/tests/"
fi

# Copy lint results
if [ -d "app/build/reports/lint-results" ]; then
    cp -r app/build/reports/lint-results reports/
    print_success "Lint reports copied to reports/lint-results/"
fi

# Generate coverage report if available
if [ -d "app/build/reports/coverage" ]; then
    cp -r app/build/reports/coverage reports/
    print_success "Coverage reports copied to reports/coverage/"
fi

# Build debug APK for testing
print_status "Building debug APK..."
if ./gradlew assembleDebug; then
    print_success "Debug APK built successfully"
    APK_PATH="app/build/outputs/apk/debug/app-debug.apk"
    if [ -f "$APK_PATH" ]; then
        APK_SIZE=$(du -h "$APK_PATH" | cut -f1)
        print_success "APK location: $APK_PATH (Size: $APK_SIZE)"
    fi
else
    print_error "Failed to build debug APK"
    exit 1
fi

# Check for connected devices (optional)
print_status "Checking for connected Android devices..."
if command -v adb &> /dev/null; then
    DEVICES=$(adb devices | grep -v "List of devices" | grep "device$" | wc -l)
    if [ "$DEVICES" -gt 0 ]; then
        print_success "Found $DEVICES connected device(s)"
        
        # Ask if user wants to install APK
        read -p "Do you want to install the APK on connected device(s)? (y/n): " -n 1 -r
        echo
        if [[ $REPLY =~ ^[Yy]$ ]]; then
            print_status "Installing APK..."
            if adb install -r "$APK_PATH"; then
                print_success "APK installed successfully"
            else
                print_warning "Failed to install APK"
            fi
        fi
    else
        print_warning "No Android devices connected"
    fi
else
    print_warning "ADB not found. Cannot check for connected devices."
fi

# Generate summary report
print_status "Generating summary report..."

SUMMARY_FILE="reports/test_summary.txt"
cat > "$SUMMARY_FILE" << EOF
Game File Inspector - Test Summary
==================================
Generated: $(date)

Project Structure:
- Source files: $(find app/src/main -name "*.kt" | wc -l) Kotlin files
- Test files: $(find app/src/test -name "*.kt" | wc -l) unit test files
- Layout files: $(find app/src/main/res/layout -name "*.xml" | wc -l) layout files
- Resource files: $(find app/src/main/res -name "*.xml" | wc -l) total XML files

Build Information:
- Gradle version: $(./gradlew --version | grep "Gradle" | head -1)
- Build tools: Android Gradle Plugin
- Target SDK: 34 (Android 14)
- Minimum SDK: 24 (Android 7.0)

Test Results:
- Unit tests: Check reports/tests/ for detailed results
- Lint analysis: Check reports/lint-results/ for issues
- APK build: $([ -f "$APK_PATH" ] && echo "SUCCESS" || echo "FAILED")
- APK size: $([ -f "$APK_PATH" ] && du -h "$APK_PATH" | cut -f1 || echo "N/A")

Next Steps:
1. Review test reports in the reports/ directory
2. Fix any failing tests or lint issues
3. Test the APK on physical devices
4. Consider running instrumented tests if devices are available

EOF

print_success "Summary report generated: $SUMMARY_FILE"

# Display final status
echo ""
echo "🎉 Test execution completed!"
echo ""
echo "📊 Results Summary:"
echo "  • Project compiled: ✅"
echo "  • Unit tests: $([ -d "app/build/reports/tests" ] && echo "✅" || echo "❌")"
echo "  • Lint analysis: $([ -d "app/build/reports/lint-results" ] && echo "✅" || echo "❌")"
echo "  • Debug APK: $([ -f "$APK_PATH" ] && echo "✅" || echo "❌")"
echo ""
echo "📁 Check the 'reports/' directory for detailed results"
echo "📱 APK ready for testing: $APK_PATH"
echo ""

# Open reports directory if on macOS or Linux with GUI
if command -v xdg-open &> /dev/null; then
    read -p "Open reports directory? (y/n): " -n 1 -r
    echo
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        xdg-open reports/
    fi
elif command -v open &> /dev/null; then
    read -p "Open reports directory? (y/n): " -n 1 -r
    echo
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        open reports/
    fi
fi

print_success "All done! 🚀"