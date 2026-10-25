# LockNova

LockNova is a **native Android app** exploring a Good Lock-style customization hub for ColorOS and other Android phones. The UI uses a soft lavender/light theme.

## What Samsung Good Lock does

Samsung's official Good Lock suite is a launcher for optional modules, not one universal settings screen. Samsung documents features including:

- **QuickStar:** status-bar indicators and Quick Panel appearance/layout.
- **LockStar and ClockFace:** lock-screen and Always On Display layouts, clocks, widgets and decoration.
- **Home Up:** home/app drawer/folder layouts, recent-apps style and share UI.
- **NavStar and One Hand Operation+:** navigation bar and gesture customisation.
- **Theme Park:** themes, icons, keyboard, Quick Settings and volume panel styling.
- **Keys Cafe / Sound Assistant:** keyboard and audio customisation.
- **Wonderland / Edge Lighting+:** animated wallpapers and notification effects.
- **RegiStar / NotiStar / Nice Shot:** settings shortcuts, notification history, and screenshot/screen-recording conveniences.

Sources: [Samsung's Good Lock feature guide](https://www.samsung.com/in/support/mobile-devices/explore-good-lock-and-how-its-applications-enhance-your-device/) and [Google Play listing](https://play.google.com/store/apps/details?id=com.samsung.android.goodlock).

## What this version actually does

- Native Android Activity and touch-friendly interface (not a web page wrapper).
- Saves the user's chosen LockNova accent colour locally.
- Opens supported Android settings pages for Wi-Fi, display, notifications, security, accessibility and app management.
- Applies a built-in soft gradient wallpaper through Android's WallpaperManager.
- Explains device/API limits rather than displaying fake system toggles.

## Important ColorOS limitation

Good Lock's deepest features rely on Samsung's own One UI modules and privileged integration. A normal third-party app cannot simply replace Oppo's global status bar, Quick Settings panel, lock screen clock, recent-apps UI, or system gesture implementation. Those features require supported OEM APIs, system/signature permissions, or a different approach such as a custom launcher. LockNova does **not** claim those protected changes are already implemented.

The project targets Android 8.0+ and is intended to run on Android 11 / ColorOS. Manufacturer-specific settings intents can vary.

## Build locally

Requires JDK 17, Android SDK platform 35, and Gradle 8.9.

    gradle assembleDebug

APK output: app/build/outputs/apk/debug/app-debug.apk.

## GitHub Actions CI/CD

Workflow: [.github/workflows/android-ci.yml](.github/workflows/android-ci.yml)

- Builds on pushes to main, pull requests, manual dispatch, and version tags.
- Uploads the debug APK as a downloadable Actions artifact.
- On a pushed tag such as v0.1.0, publishes the APK to a GitHub Release.

No signing secrets are required for a debug APK. A production Play Store build should use a protected signing key and a separate release configuration.
