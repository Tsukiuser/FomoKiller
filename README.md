# 📱 FomoKiller — Smart Open-Source Notification Blocker for Android

[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)
[![Platform](https://img.shields.io/badge/Platform-Android-green.svg)](https://www.android.com/)
[![Status](https://img.shields.io/badge/Status-Beta-orange.svg)]()
[![Privacy](https://img.shields.io/badge/Privacy-100%25_Local-brightgreen.svg)]()

**FomoKiller** is a free, ad-free, and open-source **Android notification manager and focus app**. It gives you back control over your digital wellbeing by automatically filtering out distracting notifications without making you miss urgent calls or important messages.

---

## 🎯 Why FomoKiller?

Traditional **"Do Not Disturb" (DND)** modes are too binary: either you get interrupted constantly, or you block everything and feel anxious about missing an emergency (FOMO — *Fear Of Missing Out*). 

**FomoKiller** bridges that gap. It acts as a smart firewall for your notifications, keeping your phone quiet while ensuring critical alerts always reach you—100% locally on your device.

---

## ✨ Key Features

*   **⚙️ Three Custom Focus Modes:**
    *   **Disabled:** Standard behavior. All notifications come through normally.
    *   **Selective Mode:** Blocks only distracting apps (social media, games, shopping) while letting everything else through.
    *   **Protected (VIP) Mode:** Maximum focus. Blocks *all* notifications except your chosen VIP apps (family, work, banking) and incoming phone calls.
*   **🔍 Smart Keyword Filtering:** Fine-tune your focus by automatically allowing or blocking notifications containing specific keywords (e.g., *"Urgent"*, *"Code"*, *"Delivery"*).
*   **⚡ Deep Android Integration:** Toggle focus modes effortlessly via **Quick Settings tiles** and **App Shortcuts**.
*   **🤖 Automation & Voice Ready:** Compatible with **Gemini**, **Bixby**, **Google Assistant**, **Samsung Routines**, and **Tasker**.
*   **🛡️ 100% Offline & Private:** Zero cloud servers, zero analytics, zero tracking. Your notification data never leaves your smartphone.

---

## 📥 How to Install

### Option 1: Direct APK Download (Recommended for Users)
1. Go to the [Releases Page](https://github.com/Tsukiuser/FomoKiller/releases).
2. Download the latest `.apk` file onto your Android device.
3. Open the file to install it (enable *"Install from unknown sources"* if prompted).
4. **First Launch:** Grant the **Notification Access** permission when guided by the app so it can filter your alerts.

> **⚠️ Installation Note (Google Play Protect):** Because FomoKiller is currently in **Beta** and uses an unsigned APK, Google Play Protect may display a warning during installation. Simply tap **"More details"** (or expand the message) and select **"Install anyway"**. You can verify the APK's safety anytime via [VirusTotal](https://www.virustotal.com/).

### Option 2: Build from Source (For Developers)
1. Clone this repository:
   ```bash
   git clone [https://github.com/Tsukiuser/FomoKiller.git](https://github.com/Tsukiuser/FomoKiller.git)

2. Open the project in **Android Studio**.
3. Build and install the debug APK:
   ```bash
   ./gradlew assembleDebug



---

## ⚙️ Recommended Setup (Battery Optimization)

To prevent Android from killing FomoKiller in the background, disable battery optimization for the app. *(FomoKiller uses virtually zero battery).*

1. **Long-press** the FomoKiller app icon on your home screen.
2. Tap **App info** (or the **ⓘ** icon).
3. Go to **Battery** (or *App battery usage*).
4. Select **Unrestricted**.

*Need help for your specific phone brand (Samsung, Xiaomi, OnePlus, etc.)? Check [Don't Kill My App](https://dontkillmyapp.com/).*

---

## 🔒 Privacy & Permissions

Privacy isn't an option; it's the core architecture of FomoKiller:

* **No Data Collection:** Your notifications are processed in real-time in your device's memory and are never saved or sent anywhere.
* **Minimal Permissions:** Aside from *Notification Access*, the app only requests the `INTERNET` permission strictly to check for new GitHub releases and download updates.
* **Auditable Code:** Fully open-source under the MIT License.

---

## ⚠️ Known Limitations & Beta Disclaimer

* **Beta Software:** FomoKiller is under active development. Bugs or unexpected crashes may occur.
* **Android Version Differences:** Filtering behavior may vary slightly depending on your Android version (especially on Android 11 and above) and manufacturer skin (OneUI, MIUI, Pixel UI).

---

## 🛠️ Technical Architecture (For Developers)

### Tech Stack

* **Language:** Kotlin
* **UI Framework:** XML with ViewBinding & Material Design 3 (BottomSheet for app selection)
* **Core Engine:** Android `NotificationListenerService` for real-time notification interception
* **State & Persistence:** Singleton pattern (`AppState`) backed by `SharedPreferences`
* **System Components:** `BroadcastReceiver` (`BootReceiver`) to automatically restore service state after a device reboot

### Project Structure

```text
fomokiller/
├── app/
│   ├── src/main/
│   │   ├── java/com/fomokiller/
│   │   │   ├── MainActivity.kt            # Main UI, permissions & BottomSheet triggers
│   │   │   ├── FomoNotificationService.kt # Core NotificationListenerService logic
│   │   │   ├── AppState.kt                # Global state & SharedPreferences manager
│   │   │   └── BootReceiver.kt            # Restarts the filtering service on boot
│   │   └── res/
│   │       ├── layout/                    # XML layouts (Main, BottomSheet, List Items)
│   │       └── values/                    # Material 3 Themes, Colors, Strings
└── README.md

```

---

## 🤝 Contributing & License

Contributions, bug reports, and feature requests are welcome! Feel free to open an issue or submit a **Pull Request** (note that due to time constraints, PR reviews may take a little while).

Distributed under the **MIT License**. See the `LICENSE` file for details.

---

*Built with ❤️ to help you reclaim your focus and time.*

```


```
