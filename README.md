# Game File Inspector Project

This repository hosts the "Game File Inspector" Android application.

## Project Details

The main project code and detailed documentation are located in the `GameFileInspector/` directory.

**Please refer to the [GameFileInspector/README.md](GameFileInspector/README.md) for comprehensive information about the application, its features, and how to build and contribute.**

## CI/CD

This project employs a hybrid CI/CD approach:

-   **GitHub Actions** is used for building the official unsigned release APKs and creating GitHub Releases when version tags (e.g., `v1.0.0`) are pushed. The workflow for this is defined in `GameFileInspector/.github/workflows/android-release-apk.yml`. This is the primary source for user-facing releases.

-   **GitLab CI/CD** handles other continuous integration tasks, such as running tests on pushes and merge requests, building debug APKs, nightly builds, and integrated security scans (like secret detection). This configuration is defined in `GameFileInspector/.gitlab-ci.yml`.

Build artifacts from both systems can be found in their respective "Actions" or "CI/CD" sections on GitHub and GitLab.

## Getting Started

1.  Clone this repository:
    ```bash
    git clone <repository-url>
    cd <repository-name>
    ```
2.  Navigate to the project directory:
    ```bash
    cd GameFileInspector
    ```
3.  Follow the instructions in [GameFileInspector/README.md](GameFileInspector/README.md) and [GameFileInspector/build_instructions.md](GameFileInspector/build_instructions.md) to build and run the project.

Thank you for your interest in Game File Inspector!
