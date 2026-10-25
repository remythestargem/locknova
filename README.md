# LockNova ✦

A soft, light-mode Good Lock-inspired customization playground for ColorOS users.

**Public repository:** https://github.com/remythestargem/locknova

## Try it

Open `index.html` in a browser, or enable GitHub Pages for this repository (Settings → Pages → Deploy from a branch → `main` → `/ (root)`). Once Pages finishes deploying, the site will be available at:

`https://remythestargem.github.io/locknova/`

## Features

- Light-only interface with soft lavender, white, mint and sky-blue surfaces.
- Responsive mobile-first dashboard and bottom navigation.
- Six interactive modules: Status Bar, Quick Panel, Lock Screen, Home Screen, Gestures and System UI.
- Live mock previews, toggles, choice chips and sliders.
- Browser-local settings persistence.
- Local settings backups and restore/reset actions.
- No account, backend, API key or build step required.

## Important limitation

This is a **web prototype**. It does not modify Android or ColorOS system settings, replace the actual status bar, or grant Android permissions. Those capabilities would require a native Android implementation and depend on the device, Android version, ColorOS APIs and granted permissions.

## Development

The prototype is intentionally dependency-free: edit `index.html` and refresh the browser. Settings are stored in `localStorage` under `locknova-settings`; local mock backups use `locknova-backups`.

## License

MIT
