## 🎵 LiveMedia: Always-On Media Control

## 🌟 Project Goal
The goal of **LiveMedia** is to transform standard media playback into a **persistent, glanceable, and interactive experience**. This application is designed exclusively for **Android 16 and newer**, with a preference for Google Pixel phones.

LiveMedia uses promoted ongoing notifications to create a "Live Activity" effect, bringing controls and track information directly to the screen. Through the in-app settings, you can customize which elements are displayed, such as album art, artist and album names, and playback controls.

It does not show any live notifications on the lock screen or Quick Settings to avoid duplication with the default media player.

---

## 🛠️ Code Description

The codebase is organized into the following key modules:

- **`lockscreen`**: Manages the visibility of the media controls on the lock screen.
- **`media`**: Handles media state management, including the `MusicState` data class.
- **`notification`**: Contains the `MediaNotificationListenerService` for intercepting media notifications and the `NotificationUpdateScheduler` for updating the custom notification.
- **`settings`**: Manages Quick Settings state and disable notifications when Quick Settings are opened.
- **`ui`**: Defines the Jetpack Compose UI. This includes the `PermissionScreen` for handling permission requests, the `SettingsScreen` for customizing notification content.
- **`storage`**: Manages persistent user preferences for notification content using `SharedPreferences`.
- **`utils`**: Provides utility functions for logging and notification management.

---

## 🚀 Setup and Permissions

Since LiveMedia intercepts system notifications, explicit user permission is required.

### Required Permissions

| Permission | Reason |
| :--- | :--- |
| **Notification Listener** | Required by `MediaNotificationListenerService` to access media metadata and session tokens. |
| **Accessibility Service** | Required by `QSStateListener` to detect when Quick Settings or the Notification Shade is opened, allowing the app to hide notifications during user interaction. |

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

After installation, go to **Settings** and manually grant **Notification Listener Access** and **Accessibility Service** to the LiveMedia app.
Start playing music! The LiveMedia control should appear once media playback begins and the phone is unlocked.

---

### 🎨 Customization & Supported Apps

**Status Bar Pill:**
View the [list of supported apps](https://github.com/RossSihovsk/LiveMedia/blob/main/app/src/main/java/com/ross/livemedia/media/MusicProvider.kt) that display their icons in the status bar pill.

**Notification Settings:**
Tailor the media notification to your preference. You have full control over the content and format, allowing you to toggle specific elements—such as album art, artist name, action buttons, and progress bars—to create a cleaner or more detailed look.
<img width="1024" height="751" alt="image" src="https://github.com/user-attachments/assets/4d86e620-1952-44d8-a578-88bac7d13e8c" />


![LiveMedia Notification Screenshot](https://github.com/user-attachments/assets/cf87c831-1949-4d0b-b846-2747b5b00d61)

![LiveMedia Notification Screenshot](https://github.com/user-attachments/assets/049e39dc-87b9-4fa2-b2ac-b55684b72da2)

![ezgif-4220ad52c4c430a7](https://github.com/user-attachments/assets/923c0059-350d-47f4-938e-d202ba7ac369)
