# SMS Toggle Controller

Control supported Android phone settings remotely using authenticated SMS commands.

## Planned features
- One Android app; no parent/child app pair and no account registration.
- Configure one trusted sender phone number.
- Configure a secret command PIN and custom SMS keywords.
- Map commands to supported device actions such as flashlight, Bluetooth, and Do Not Disturb.
- Validate the sender and command before taking action.
- Show command results in the app; SMS replies may depend on Android permissions and carrier support.

## Android compatibility
Android restricts background SMS access and programmatic changes to sensitive settings. Airplane mode, mobile data, Wi-Fi, and Location cannot be freely toggled by an ordinary third-party app on many Android versions. Where direct control is blocked, the app must guide the user to the relevant system settings rather than claim the action succeeded.

## Development status
Project setup and implementation are in progress. Features are not yet complete or tested.

## Build
Open this project in Android Studio after the Gradle Android project files have been added.
