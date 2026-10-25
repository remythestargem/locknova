# LockNova

LockNova is an Android customization hub and an optional home-screen launcher designed for ColorOS-compatible devices. It targets Android 11 (API 30) and stays useful without root. On a rooted phone, users can separately approve a small set of root-backed Android settings; no system change happens simply by installing or opening the app.

## What it can do

- Run as a real, optional Android Home app. LockNova becomes the default only if the user chooses it in Android's Home app settings.
- Launch installed apps from a searchable app drawer; pin apps to a configurable dock; open app info from a long press.
- Customize LockNova Home's grid, icon size and shape, labels, wallpaper-backed layout, theme and accent.
- Host installed Android app widgets through Android's `AppWidgetHost` API. Multiple real provider views share one footprint; users swipe or tap the stack controls to cycle, and can add, reorder or remove widgets. The stack is not a vertical mock-up.
- Apply selected home- or lock-screen wallpapers using Android's `WallpaperManager`, with a best-effort local backup and restore action when Android exposes the current wallpaper.
- On a rooted device, optionally change Android's stored status-icon visibility and Quick Settings tile order through a restricted allowlist. LockNova asks before applying, records the previous values, verifies writes and offers restoration. ColorOS may ignore or override these Android SystemUI settings.
- Reduce Android's three global animation scales to 0.5× through root or, if root is unavailable, authorized Shizuku. The original values are read, saved, verified after changes and available for restoration.
- Save, apply, rename, duplicate and delete app appearance presets; export/import the configuration as JSON; keep a local chronological activity history.
- Preview other status-bar and Quick Settings styling ideas without claiming that previews alter ColorOS.

## Build

Requirements: JDK 17 or later, Android SDK Platform 35 and Build Tools 35.0.0. Build the debug APK and run the unit tests with:

```sh
gradle --no-daemon testDebugUnitTest :app:assembleDebug
```

APK output: `app/build/outputs/apk/debug/app-debug.apk`.

GitHub Actions runs the same test/build on pushes to `main`, pull requests and manual dispatch. Each successful run uploads a `LockNova-debug-apk` artifact. Open the repository's **Actions** tab, choose the successful **Android APK** run, and download that artifact.

## Install and choose LockNova Home

1. Download and install the APK on the phone. If Android asks, allow installation from the file manager or browser used to open the APK.
2. Open LockNova and use **Settings → Launcher → Set as default home app**, then select LockNova in Android's chooser. You can change back at any time in Android's Home app settings.
3. On the launcher, choose **Add widget**. Select a provider and approve Android's widget-binding prompt. A widget may show its provider's own configuration screen before it is added. Swipe within the single widget area or use its paging controls to cycle; use **Manage stack** to reorder or remove entries.

## Optional privileged access

Root is optional for the launcher, wallpaper tools and previews. Root-backed SystemUI actions run only after the user chooses a specific operation and confirms it. LockNova restricts root commands to a short allowlist of Android settings; it does not expose an arbitrary root shell. Android values are saved before changes and read back afterwards. Use the restore action to put saved values back. Root permission does not guarantee that ColorOS will display an AOSP SystemUI setting.

If root is unavailable, the animation-scale operation can use Shizuku:

1. Install Shizuku from its [official download page](https://shizuku.rikka.app/download/).
2. Enable Developer options and Wireless debugging on the phone.
3. In Shizuku, start pairing, choose **Pair device with pairing code** in Wireless debugging, and enter the code in the Shizuku notification. Then start Shizuku from its app.
4. Return to LockNova and request Shizuku permission. Shizuku must be restarted after a reboot when started through Wireless debugging.
5. On OPPO/ColorOS, the [official Shizuku guide](https://shizuku.rikka.app/guide/setup/) notes that Developer options → **Permission monitoring** may need to be disabled if ADB permissions are limited.

Shizuku is not root, and its shell permissions vary by Android version and OEM. LockNova checks and reports each supported operation instead of assuming access.

## Known limitations

- The launcher replaces only the Home app. It does not replace ColorOS's lock screen, clock, notification shade or system status bar.
- Actual launcher widgets are hosted by LockNova and remain subject to Android widget-binding/provider restrictions. Some providers may be incompatible or require setup.
- Root-backed status-icon and tile-order changes use Android SystemUI settings that ColorOS may ignore, override or change between firmware versions; they are not guaranteed to change what is drawn on screen.
- Global clock placement, system-panel colors, arbitrary tile creation, device-wide gestures and many ColorOS-only features are not claimed as verified operations.
- Root-backed settings and animation changes have not been verified on physical OPPO CPH1933 firmware in this build environment. ColorOS may reject or later override them.
- Wallpaper restoration is best-effort: Android or the wallpaper provider may not expose a readable original wallpaper file.
