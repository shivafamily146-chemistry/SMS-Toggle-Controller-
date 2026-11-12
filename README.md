# SMS Toggle Controller

Control supported Android phone settings remotely using authenticated SMS commands.

## App design
- One Android app; no parent/child app pair and no account registration.
- Configure one trusted sender phone number and a private command PIN.
- Accept commands only when both the sender number and PIN match.
- Current command keywords: `LIGHT`, `BLUETOOTH`, and `DND`.

## Example SMS
Replace the example PIN with the PIN saved in the app:

```text
4826 LIGHT
4826 BLUETOOTH
4826 DND
```

## Android compatibility and permissions
- Flashlight control requires camera/flash hardware and camera permission.
- Bluetooth enable/disable is restricted on modern Android versions and may be rejected by the operating system or device manufacturer.
- Do Not Disturb requires the user to grant Notification Policy access in Android Settings.
- Ordinary third-party apps cannot freely toggle Airplane Mode, mobile data, Wi-Fi, or Location on current Android versions. There is no universal SMS-only workaround that can automatically enable Location on every Android phone. This app does not claim to perform that restricted action.
- SMS reception requires the user to grant SMS permission. Availability and distribution are subject to Android and app-store policies.

## Build
The GitHub Actions workflow builds a debug APK on pushes and pull requests. Open the Actions tab, select the latest successful workflow run, and download the `app-debug-apk` artifact.

## Development status
The source is still under development. A successful CI build verifies compilation only; it does not prove that every command works on every phone. Install and test on the target device before relying on it.
