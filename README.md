# 📱 FomoKiller

**FomoKiller** is an open-source Android app designed to give you back control over your distractions. Tired of being interrupted by useless notifications while still being afraid of missing an important call? FomoKiller is here for that.

---

## ✨ Features

- **🛡️ 100% Local & Private**: No server, no data collection. Your notifications stay on your phone.
- **🚀 No frills**: A minimalist interface for maximum efficiency.
- **⚙️ Three focus modes**:
  - **Disabled**: Normal life. All notifications go through.
  - **Enabled (Selective)**: Block only the culprits (social media, games, etc.). Everything else goes through.
  - **Protected (VIP)**: The ultimate focus mode. Everything is blocked, except your VIP apps (family, work, emergencies) and system calls.

---

## 🛠️ Tech Stack

- **Language**: Kotlin
- **Architecture**: Singleton pattern for global state (`AppState`) and bound Service.
- **UI**: Material Design 3, ViewBinding, BottomSheet for app selection.
- **Core**: `NotificationListenerService` for surgical notification interception.
- **Storage**: `SharedPreferences` for ultra-lightweight persistence.

---

## 📂 Project Structure

```text
fomokiller/
├── app/
│   ├── src/main/
│   │   ├── java/com/fomokiller/
│   │   │   ├── MainActivity.kt           # Main interface & UI logic
│   │   │   ├── FomoNotificationService.kt # System core (Interception)
│   │   │   ├── AppState.kt                # Mode and preferences management
│   │   │   └── BootReceiver.kt            # Restarts the service on boot
│   │   └── res/
│   │       ├── layout/                    # XML layouts (Main, BottomSheet, Item)
│   │       └── values/                    # Themes, Colors, Strings
└── README.md
```

---

## 📥 Installation

You can install FomoKiller in two ways:

### Option 1: Direct Download (Recommended)
1. Go to the [Releases](https://github.com/Tsukiuser/FomoKiller/releases) tab.
2. Download the latest `.apk` file.
3. Install it on your smartphone (allow unknown sources if necessary).
4. If you have **installation issues** because of **Google Play Protect**, refer to [this section](https://github.com/Tsukiuser/FomoKiller#%EF%B8%8F-important-notes)

### Option 2: Build from source
1. Clone this repository.
2. Open the project in **Android Studio**.
3. Build and install (`./gradlew assembleDebug`).

> **Important note**: To work, the app requires **"Notification Access"** permission. The app will guide you to the settings on first launch.

---

## 🔒 Privacy

Privacy isn't an option, it's the foundation:
- **Zero internet access**: The app doesn't even have the `INTERNET` permission.
- **Zero Cloud**: No data ever leaves your device.
- **Open Source**: The code is transparent and auditable by everyone.

---

## 🔋 Battery Optimization

For optimal use and operation, I recommend disabling battery optimization. The app is so lightweight that the system doesn't even register it. To do this:
- **Long-press** the app icon from your home screen.
- **Tap "App info,"** or the "🛈" icon
- Scroll down to **"Battery"**
- Select **"Unrestricted"**

---

## ⚠️ Important Notes

- Please note that the app **may behave differently** depending on your Android version, particularly versions **later than Android 10**
- The app is still in **BETA**, by using it you accept that the app **may not function properly, or even crash**
- Some antivirus software may send you notifications about this app: particularly Google Play Protect, **which still lets you install it safely regardless**
- In the **BETA** version, the APK is **unsigned**, which may trigger a Google Play popup during installation: **Expand** the message, then **select "Install anyway"**

---

## 🤝 Contributing

Any **Pull Request** is welcome, I **won't be able** to inspect everything, which is why not all of them will be accepted

---

## 📄 License

Distributed under the MIT license. See `LICENSE` for more information.

---

*Developed with ❤️ for those who want to reclaim their time.*

---