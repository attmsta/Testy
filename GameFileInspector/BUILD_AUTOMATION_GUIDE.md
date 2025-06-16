# 🚀 Build Automation Guide

## Overview

This guide covers the comprehensive build automation system for Game File Inspector. The project utilizes a hybrid approach with GitHub Actions for release APK generation and GitHub Releases, and GitLab CI/CD for other continuous integration tasks.

## 🔄 Automated Build Workflows

Two CI/CD systems are in place:

### 1. GitHub Actions (`GameFileInspector/.github/workflows/android-release-apk.yml`)

This workflow is the **primary mechanism for building official unsigned release APKs and creating user-facing GitHub Releases.**

#### Workflow Purpose and Triggers
-   **Purpose**: To build, (optionally) test, and package the unsigned release APK, and then create a GitHub Release when a version tag is pushed.
-   **Triggers**:
    -   Pushes to the `main` branch.
    -   Pushes of version tags (e.g., `v1.0.0`, `v1.2.3`).
    -   Manual dispatch (`workflow_dispatch`).

#### Workflow Jobs
The workflow typically consists of two main jobs:

1.  **`build-release-apk`**:
    *   Checks out the code.
    *   Sets up JDK (version specified by `env.JAVA_VERSION`, e.g., '17').
    *   Sets up the Android SDK using `android-actions/setup-android@v3`.
    *   Caches Gradle dependencies for faster builds (paths: `~/.gradle/caches`, `~/.gradle/wrapper`, `${{ env.ANDROID_PROJECT_PATH }}/.gradle`).
    *   Grants execute permission to `gradlew`.
    *   Runs `./gradlew clean`.
    *   Optionally runs unit tests (`./gradlew testDebugUnitTest`) and lint checks (`./gradlew lintDebug`).
    *   Builds the unsigned release APK: `./gradlew assembleRelease --stacktrace`. (All Gradle commands run with `working-directory: ${{ env.ANDROID_PROJECT_PATH }}` which is `GameFileInspector`).
    *   Renames the APK to `GameFileInspector-release-unsigned.apk`.
    *   Uploads the renamed APK as an artifact named `release-apk`.

2.  **`create-github-release`**:
    *   Runs only if the workflow was triggered by a version tag (e.g., `refs/tags/v*`).
    *   Depends on the successful completion of `build-release-apk`.
    *   Downloads the `release-apk` artifact.
    *   Uses `softprops/action-gh-release@v2` to create a new GitHub Release, using the tag name for the release version and attaching the downloaded APK.

#### Accessing Build Artifacts and Releases
-   **Release APKs (Official)**: Available from the **GitHub Releases** page of the repository.
-   **Build Artifacts (Unsigned APKs from any run)**: Can be downloaded from the summary page of a specific workflow run under the "Actions" tab of the GitHub repository.

### 2. GitLab CI/CD (`GameFileInspector/.gitlab-ci.yml`)

This pipeline handles **general continuous integration tasks** beyond the official release APK build.

#### Pipeline Purpose and Triggers
-   **Purpose**: Automated testing, building debug/other APKs, nightly builds, and integrated security scans.
-   **Triggers**: Typically configured for pushes to all branches, merge requests, and scheduled runs.

#### Pipeline Stages & Key Features (as configured in `GameFileInspector/.gitlab-ci.yml`)
-   **`prepare`**: Sets up the build environment, Android SDK, and caches dependencies.
-   **`test`**: Runs unit tests (`./gradlew test`), lint checks (`./gradlew lint`), and a basic `security_scan` job.
-   **`secret-detection`**: Uses the included GitLab template for secret detection.
-   **`build`**: Compiles debug APKs (`build_debug` job), potentially release APKs for internal use (from `build_release` job if not solely for GitHub Actions tagged releases), and handles `nightly_builds`.
-   **`release`**: The `release` job in GitLab CI can create GitLab Releases (e.g., for internal previews or if triggered by `main` branch pushes, distinct from the official GitHub Releases).
-   **Other Jobs**: May include performance tests, cleanup jobs, etc.

#### Accessing Build Artifacts and Logs
-   **Build Logs**: Available from the "CI/CD" > "Pipelines" section of the GitLab repository for each pipeline run.
-   **APKs and Other Artifacts**:
    -   Can be downloaded directly from completed jobs in a GitLab pipeline run.
    -   If the GitLab `release` job is configured to create them, GitLab Releases might also contain APKs (these would typically be for internal/preview purposes).

## 🛠️ Local Build Script (`build_apk.sh`)

(This section remains largely the same as it describes local building, but context should imply it's for development, not official releases which are via GitHub Actions.)

### Features
The local build script (`build_apk.sh`, if present, or manual Gradle commands) provides capabilities for local APK building:
```bash
# Example: Build debug APK using Gradle wrapper (from GameFileInspector directory)
./gradlew assembleDebug

# Example: Build release APK using Gradle wrapper (from GameFileInspector directory)
./gradlew assembleRelease
```
*(If a `build_apk.sh` script exists, its specific commands and options should be documented here.)*

## 📦 Release Management

### Official Releases (via GitHub Actions)
-   **Primary Method**: Official, user-facing releases are created via the **GitHub Actions workflow** (`android-release-apk.yml`).
-   **Trigger**: Pushing a version tag (e.g., `v1.2.3`) to the GitHub repository.
-   **Process**: The `create-github-release` job automatically drafts a new GitHub Release, names it after the tag, and attaches the unsigned release APK built by the `build-release-apk` job.
-   **Access**: Published releases are available on the **GitHub Releases** page.

### Internal/Preview Releases (Potentially via GitLab CI/CD)
-   The GitLab CI/CD pipeline (`GameFileInspector/.gitlab-ci.yml`) also has a `release` job that can create GitLab Releases. These can be used for:
    -   Internal testing builds from the `main` branch.
    -   Nightly release previews.
-   These are distinct from the official GitHub Releases.

## 🔧 Build Configuration (Gradle)

(This section remains the same, as Gradle configuration is consistent for both CI systems and local builds.)
```gradle
// Example from GameFileInspector/app/build.gradle
android {
    compileSdk 34
    // ... other configs
    buildTypes {
        release {
            minifyEnabled false // As per workflow, for unsigned release.
            // Proguard and signing would be configured here for signed releases.
        }
    }
}
```

## 🧪 Testing Integration

-   **GitHub Actions**: The `android-release-apk.yml` workflow includes optional steps for running unit tests (`testDebugUnitTest`) and lint checks (`lintDebug`) before building the release APK.
-   **GitLab CI/CD**: The `.gitlab-ci.yml` pipeline has a dedicated `test` stage that runs unit tests, lint checks, and a basic security scan. It also includes a `secret-detection` stage.

## 🔒 Security and Signing

### Security Measures
-   **GitHub Actions**: Relies on standard secure practices; secrets (like `GITHUB_TOKEN`) are managed by GitHub.
-   **GitLab CI/CD**:
    -   Integrates secret detection via the `secret-detection` stage.
    -   Includes a `security_scan` job for basic checks.

### APK Signing
-   The **GitHub Actions workflow (`android-release-apk.yml`) currently builds an unsigned release APK** (`GameFileInspector-release-unsigned.apk`).
-   The **GitLab CI/CD pipeline (`GameFileInspector/.gitlab-ci.yml`) also builds an unsigned release APK** in its `build_release` job.
-   For distributing signed APKs (e.g., via Google Play Store), manual signing or a separate, secure signing process/workflow would be required. This typically involves securely managing keystore files and credentials.

## 📚 Quick Reference

### Important Files
-   `GameFileInspector/.github/workflows/android-release-apk.yml`: **Primary GitHub Actions workflow for official release APK builds and GitHub Releases.**
-   `GameFileInspector/.gitlab-ci.yml`: GitLab CI/CD pipeline for general CI tasks (testing, debug builds, nightly builds, security scans).
-   `GameFileInspector/build.gradle` & `GameFileInspector/app/build.gradle`: Gradle build scripts.
-   `GameFileInspector/gradlew`: Gradle wrapper script.

### Support Resources
-   **Documentation**: This guide and other markdown files in the repository.
-   **GitHub Issues**: For bug reports and feature requests related to the project.
-   **GitHub Actions Logs**: Detailed logs for release APK builds and GitHub Release creation.
-   **GitLab CI/CD Logs**: Detailed logs for other CI tasks.

This dual CI system allows for robust continuous integration via GitLab CI and a focused release pipeline via GitHub Actions.
