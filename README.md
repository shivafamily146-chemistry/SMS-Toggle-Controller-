# SMS Toggle Controller

Control supported Android phone settings remotely using authenticated SMS commands.

## App design
- One Android app; no parent/child app pair and no account registration.
- Configure one trusted sender phone number and a private command PIN.
- Accept commands only when both the sender number and PIN match.
- Current command keywords: `LIGHT`, `BLUETOOTH`, `DND`, and `LOCATION`.

## Example SMS
Replace the example PIN with the PIN saved in the app:

```text
4826 LIGHT
4826 BLUETOOTH
4826 DND
4826 LOCATION
```

Sending `LOCATION` toggles the current Location mode: OFF becomes ON (high accuracy); any enabled mode becomes OFF.

## One-time Location setup (ADB)
Location control requires the special `WRITE_SECURE_SETTINGS` permission. Android does not let an ordinary app grant this permission to itself.

1. Install the app on the phone.
2. Enable Developer options and USB debugging on the phone.
3. Connect the phone to a computer with Android Platform Tools (ADB) installed, and approve the computer's USB debugging prompt.
4. Run this command on the computer:

```bash
adb shell pm grant com.shivafamily.smstogglecontroller android.permission.WRITE_SECURE_SETTINGS
```

5. After the grant succeeds, SMS commands can request Location ON/OFF without opening Location Settings.

The ADB grant is a one-time setup for that installation. If the app is uninstalled, the permission must be granted again. Some Android versions or manufacturer builds may still block or ignore changes to the legacy Location mode setting; therefore this is not guaranteed on every phone. Test on the target device.

## Android compatibility and permissions
- Flashlight control requires camera/flash hardware and camera permission.
- Bluetooth enable/disable is restricted on modern Android versions and may be rejected by the operating system or device manufacturer.
- Do Not Disturb requires the user to grant Notification Policy access in Android Settings.
- Location requires the one-time ADB grant described above; it is not a universal no-setup feature.
- Ordinary third-party apps cannot freely toggle Airplane Mode, mobile data, or Wi-Fi on current Android versions.
- SMS reception requires the user to grant SMS permission. Availability and distribution are subject to Android and app-store policies.

## Build
The GitHub Actions workflow builds a debug APK on pushes and pull requests. Open the Actions tab, select the latest successful workflow run, and download the `app-debug-apk` artifact.

## Development status
The source is still under development. A successful CI build verifies compilation only; it does not prove that every command works on every phone. Install and test on the target device before relying on it.
