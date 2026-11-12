# Device Controller

A simple, on-device Android utility for controlling supported device features.

## Features
- Toggle the flashlight from inside the app (requires camera permission and flash hardware).
- Toggle Do Not Disturb after the user grants Notification Policy access.
- Open Android's own settings pages for Bluetooth, Wi-Fi, Location, Airplane mode, and network/mobile data.

## Privacy and permissions
- This Play Store-oriented build does not read, receive, send, or process SMS messages.
- It does not require account registration or collect a trusted phone number.
- It requests Camera permission only for flashlight control.
- Do Not Disturb access is optional and is requested only when the user taps the DND control.
- Features that Android does not allow ordinary apps to change directly are opened in system settings for the user to control.

## Compatibility
The app supports Android 6.0 (API 23) and later. Availability of individual settings screens and hardware features varies by device manufacturer and Android version. This app does not claim that every setting can be changed automatically on every phone.

## Google Play preparation
New apps submitted to Google Play from August 31, 2026 must target Android 16 (API 36) or higher. This project targets API 36. Store publication is still subject to Google Play review and all applicable policies.

## Build
GitHub Actions builds a debug APK when changes are pushed to the main branch. A successful build confirms compilation; it does not replace testing on real devices.
