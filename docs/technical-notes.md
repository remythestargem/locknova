# Implementation notes and official references

## Android app-widget hosting

LockNova Home is a home-screen replacement, so it can host app widgets using Android's `AppWidgetHost` API. The host has a stable host ID, allocates and persists widget IDs, checks whether it may bind each selected provider, presents Android's binding-permission flow when needed, launches a provider's required configuration activity, calls `deleteAppWidgetId()` when a widget is removed, and starts/stops listening with the launcher lifecycle. A widget stack is a single fixed-size `FrameLayout` containing the real host views: exactly one provider view is visible at a time, while paging controls and deliberate horizontal swipes cycle between them. Views are cached so provider state and tap actions remain live. The app's settings hub does not show a pretend widget carousel.

Reference: [Build a widget host — Android Developers](https://developer.android.com/develop/ui/views/appwidgets/host). The guide describes home-screen replacements as a valid widget-host use case and covers explicit binding permission, provider configuration and host cleanup.

## Optional root-backed settings

Root access is optional and is checked only when a user invokes a root-backed operation. `RootBridge` allows root execution of a tiny fixed set of Android settings keys: the three global animation scales, secure `icon_blacklist` and secure `sysui_qs_tiles`. It accepts no arbitrary user shell command, restricts namespace and key, bounds command time and safely quotes values.

Every operation that can write a system value is preceded by an in-app confirmation. Original secure SystemUI values are retained in a private recovery record; animation-scale values use a separate record. Writes are read back, and a failed readback triggers a rollback attempt while keeping the recovery record. A restore action writes and verifies the saved values before clearing the record. Storing an AOSP SystemUI setting does not guarantee that an OEM skin such as ColorOS will honor or display it.

## Shizuku fallback

The app uses the official `dev.rikka.shizuku:api` and `dev.rikka.shizuku:provider` artifacts and manifest provider. Binder availability, Shizuku app installation, and per-app authorization are treated as separate states. The app listens for binder receive/death and permission results, and checks authorization before a privileged operation. Shizuku does not imply root; shell permissions vary by Android version and OEM.

References: [Shizuku-API developer guide](https://github.com/RikkaApps/Shizuku-API/blob/master/README.md); [Shizuku Android source API](https://github.com/RikkaApps/Shizuku-API/blob/master/api/src/main/java/rikka/shizuku/Shizuku.java).

The supported Android 11 non-root setup uses Wireless debugging and needs to be repeated after reboot. The official troubleshooting guide notes that OPPO/OnePlus ColorOS may require turning off Developer options → Permission monitoring if ADB permissions are limited.

Reference: [Shizuku user setup and FAQ](https://shizuku.rikka.app/guide/setup/).

When root is not granted, LockNova may use Shizuku for the three global animation-scale settings. It reads values first, stores original values before writes, requires UI confirmation, applies constant key/value arguments, reads back to verify, attempts rollback on partial failure, and retains a restoration record until restoration succeeds.
