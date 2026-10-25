# LockNova

LockNova is a Java Android customization hub and an optional Android home-screen launcher for ColorOS-compatible devices. It targets Android 11 (API 30), uses native Android views, stores settings locally, and requires neither root nor an account/backend.

## What it can do

- Run as a real, optional Android Home app. LockNova does not become default until the user selects it in Android's Home app settings.
- Launch installed apps from a searchable app drawer; pin apps to a configurable dock; open app info from a long press.
- Customize launcher grid columns, icon size and shape, labels, theme and accent.
- Host installed Android app widgets with Android's `AppWidgetHost` API. Users choose each widget, grant Android's binding permission, complete the provider's setup if required, then swipe smoothly through the real widgets in a stack. The stack can be reordered and widgets removed.
- Apply selected home- or lock-screen wallpapers using Android's `WallpaperManager`, with a best-effort local backup and restore action when Android exposes the current wallpaper.
- Use Shizuku for one narrow system operation: reduce Android's three global animation scales to 0.5×. LockNova reads and records the previous values, asks before applying, reads back to verify, attempts rollback after failures and offers restoration.
- Save, apply, rename, duplicate and delete app appearance presets; export/import the configuration as JSON; keep a local chronological activity history.
- Preview status-bar and Quick Settings concepts without claiming to change ColorOS's actual system panels.

## Build

Requirements: JDK 17 or later, Gradle 8.11.1, Android SDK Platform 35 and Build Tools 35.0.0. Build the debug APK and run the unit test with:

```sh
gradle --no-daemon testDebugUnitTest :app:assembleDebug
```

APK output: `app/build/outputs/apk/debug/app-debug.apk`.

GitHub Actions installs the pinned Gradle and Android SDK versions, runs the same test/build on pushes to `main`, pull requests and manual dispatch, then uploads a `LockNova-debug-apk` artifact for 14 days. Open the repository's **Actions** tab, choose a successful **Android APK** run, and download that artifact.

## Install and choose LockNova Home

1. Download and install the APK on the phone. If Android asks, allow installation from the file manager or browser used to open the APK.
2. Open LockNova and use **Settings → Launcher → Set as default home app**, then select LockNova in Android's chooser. You can change back at any time in Android's Home app settings.
3. On the launcher, choose **Add widget**. Select a provider and approve Android's widget-binding prompt. A widget may show its provider's own configuration screen before it is added. Swipe the widget area to cycle the stack; use **Manage stack** to reorder or remove entries.

## Set up Shizuku on Android 11

1. Install Shizuku from its [official download page](https://shizuku.rikka.app/download/).
2. Enable Developer options and Wireless debugging on the phone.
3. In Shizuku, start pairing, choose **Pair device with pairing code** in Wireless debugging, and enter the code in the Shizuku notification. Then start Shizuku from its app.
4. Return to LockNova and request Shizuku permission. Shizuku must be restarted after a reboot when started through Wireless debugging.
5. On OPPO/ColorOS, the [official Shizuku guide](https://shizuku.rikka.app/guide/setup/) notes that Developer options → **Permission monitoring** may need to be disabled if ADB permissions are limited.

Shizuku is not root. A permission grant does not guarantee that a ColorOS system operation will be allowed; LockNova checks and reports each result.

## Known limitations

- The launcher replaces only the Home app. It does not replace ColorOS's lock screen, status bar or notification shade.
- Actual launcher widgets are hosted by LockNova and remain subject to Android widget binding/provider restrictions. Some providers may be incompatible or require setup.
- Status bar placement/icon styling, actual Quick Settings tile order/colors, global gestures, arbitrary launcher icons in other launchers, and many ColorOS-only features are preview-only or native-settings shortcuts.
- The Shizuku animation-scale operation has not been verified on physical OPPO CPH1933 firmware in this build environment. ColorOS may reject or later override it.
- Wallpaper restoration is best-effort: Android or the wallpaper provider may not expose a readable original wallpaper file.
- LockNova intentionally does not request root, accessibility, location, contacts or message access and does not run a persistent background service.
