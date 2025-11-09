## 🎵 LiveMedia: Always-On Media Control

## 🌟 Project Goal

The goal of **LiveMedia** is to transform standard media playback into a **persistent, glanceable, and interactive experience** on your Android 16 device.

LiveMedia uses promoted ongoing notifications to create a "Live Activity" effect, bringing controls and track information directly to the screen.

It does not show any live notifications on the lock screen to avoid duplication with the default media player. (But I was not able to avoid it in QS)

---

## 🛠️ Code Description

The codebase is organized into the following key modules:

- **`lockscreen`**: Manages the visibility of the media controls on the lock screen.
- **`media`**: Handles media state management, including the `MusicState` data class.
- **`notification`**: Contains the `MediaNotificationListenerService` for intercepting media notifications and the `NotificationUpdateScheduler` for updating the custom notification.
- **`settings`**: Manages Quick Settings tile integration and System UI state. (Currently disabled)
- **`ui`**: Defines the Jetpack Compose UI theme and components.
- **`utils`**: Provides utility functions for logging and notification management.

---

## 🚀 Setup and Permissions

Since LiveMedia intercepts system notifications and UI events, explicit user permission is required.

### Required Permissions

| Permission | Reason |
| :--- | :--- |
| **Notification Listener** | Required by `MediaNotificationListenerService` to access media metadata and session tokens. |

### Installation

  **Download APK (Recommended for Quick Setup):**
    * Navigate to the **[Releases](https://github.com/RossSihovsk/LiveMedia/releases)** tab of this repository.
    * Download the latest **`release.apk`** file directly to your device.
    * You will need to use APKMirror to install it or to disable Play Protect.

***OR***

**Clone the repository:**
    ```
    git clone https://github.com/RossSihovsk/LiveMedia.git
    ```
  *Open the project in Android Studio and build the APK.
  *Install the app on your device.

After installation, go to **Settings** and manually grant both **Notification Listener Access** and **Accessibility Service** permissions to the LiveMedia app.
Start playing music! The LiveMedia control should appear once media playback begins and the phone is unlocked.

---
![LiveMedia Notification Screenshot](https://github.com/user-attachments/assets/cf87c831-1949-4d0b-b846-2747b5b00d61)

![LiveMedia Notification Screenshot](https://github.com/user-attachments/assets/049e39dc-87b9-4fa2-b2ac-b55684b72da2)

![LiveMedia Demo GIF](https://github.com/user-attachments/assets/736fd1c8-72e2-40a5-8ba8-0edea7c0f548)

