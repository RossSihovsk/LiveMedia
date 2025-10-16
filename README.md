# 🎵 LiveMedia: Always-On Media Control

## 🌟 Project Goal

The goal of **LiveMedia** is to transform standard media playback into a **persistent, glanceable, and interactive experience** on your Android 16 device.

LiveMedia uses promoted ongoing notifications to create a "Live Activity" effect, bringing controls and track information directly to the screen.

It does not show any live notifications on the lock screen to avoid duplication with the default media player. (But I was not able to avoid it in QS)

---

## 🛠️ Key Components

### 1. MediaNotificationListenerService (`MediaListenerService.kt`)

This is the core service that manages the notification and media commands.

| Component | Responsibility | API Used |
| :--- | :--- | :--- |
| **Media Monitoring** | Listens to all active media playback sessions. | `NotificationListenerService` |
| **Command Sending** | Sends playback commands (e.g., Pause) back to the source player. | `MediaController.TransportControls` |
| **Lock Detection** | Registers a receiver to handle device lock/unlock events. | `BroadcastReceiver` |

---

## 🚀 Setup and Permissions

Since LiveMedia intercepts system notifications and UI events, explicit user permission is required.

### Required Permissions

| Permission | Reason |
| :--- | :--- |
| **Notification Listener** | Required by `MediaNotificationListenerService` to access media metadata and session tokens. |

### Installation

1.  Clone the repository:
    ```bash
    git clone https://github.com/RossSihovsk/LiveMedia.git
    ```
2.  Open the project in Android Studio and build the APK.
3.  Install the app on your device.
4.  After installation, go to **Settings** and manually grant both **Notification Listener Access** and **Accessibility Service** permissions to the LiveMedia app.
5.  Start playing music! The LiveMedia control should appear once media playback begins and the phone is unlocked.

---
![LiveMedia Notification Screenshot](https://github.com/user-attachments/assets/cef1f757-07f1-489e-b7b7-a87a7ea22991)
![LiveMedia Notification Screenshot](https://github.com/user-attachments/assets/bb674e7a-74d8-48e1-bd34-eece364aca9f)
![LiveMedia Demo GIF](https://github.com/user-attachments/assets/b20d2f2c-aca7-4bc5-8ee4-7146a7b42979)
