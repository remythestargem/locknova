# Implementation notes and official references

## Android app-widget hosting

LockNova Home is a home-screen replacement, so it can host app widgets using Android's `AppWidgetHost` API. The host has a stable host ID, allocates and persists widget IDs, checks whether it may bind each selected provider, presents Android's binding-permission flow when needed, launches a provider's required configuration activity, calls `deleteAppWidgetId()` when a widget is removed, and starts/stops listening with the launcher lifecycle. The LockNova widget stack displays one real host view at a time and lets the user swipe, reorder and remove entries.

Reference: [Build a widget host — Android Developers](https://developer.android.com/develop/ui/views/appwidgets/host). The guide emphasizes that home-screen replacements are a valid widget-host use case, binding requires explicit user permission when not already allowed, and the host is responsible for allocation, configuration and cleanup.

## Shizuku integration

The app uses the official `dev.rikka.shizuku:api` and `dev.rikka.shizuku:provider` artifacts and manifest provider. Binder availability, Shizuku app installation, and per-app authorization are treated as separate states. The app listens for binder receive/death and permission results, and checks authorization before a privileged operation. Shizuku does not imply root; shell permissions vary by Android version and OEM.

References: [Shizuku-API developer guide](https://github.com/RikkaApps/Shizuku-API/blob/master/README.md); [Shizuku Android source API](https://github.com/RikkaApps/Shizuku-API/blob/master/api/src/main/java/rikka/shizuku/Shizuku.java).

The supported Android 11 non-root setup uses Wireless debugging and needs to be repeated after reboot. The official troubleshooting guide notes that OPPO/OnePlus ColorOS may require turning off Developer options → Permission monitoring if ADB permissions are limited.

Reference: [Shizuku user setup and FAQ](https://shizuku.rikka.app/guide/setup/).

LockNova's single privileged operation is limited to the three global animation-scale keys. It reads values first, stores original values before writes, requires a confirmation in the UI, applies constant key/value arguments, reads back to verify, attempts rollback on partial failure, and retains a restoration record until restoration succeeds. It does not execute user-provided shell commands.
