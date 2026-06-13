# FomoKiller

Application Android ultra-minimaliste de contrôle des notifications.

## 3 modes

| Mode | Icône | Comportement |
|------|-------|-------------|
| **Désactivé** | — | Toutes les notifications passent normalement |
| **Tout Bloquer** | 🔕 | Bloque les notifications des apps sélectionnées |
| **VIP Seulement** | ⭐ | Ne laisse passer que les appels, SMS, alarmes + apps VIP |

**Appui long** sur "Tout Bloquer" ou "VIP Seulement" pour choisir les apps concernées.

---

## Compiler l'APK (5 minutes)

### Prérequis
- [Android Studio](https://developer.android.com/studio) (gratuit) — ou JDK 17+ si vous préférez la ligne de commande

### Option A — Android Studio (recommandé)

1. Ouvrir Android Studio
2. **File → Open** → sélectionner le dossier `fomokiller`
3. Attendre la synchronisation Gradle (première fois : télécharge ~500MB de dépendances)
4. **Build → Build Bundle(s) / APK(s) → Build APK(s)**
5. L'APK est dans `app/build/outputs/apk/debug/app-debug.apk`
6. Copier l'APK sur votre téléphone et installer (activer "Sources inconnues" si nécessaire)

### Option B — Ligne de commande

```bash
cd fomokiller
./gradlew assembleDebug
# APK généré : app/build/outputs/apk/debug/app-debug.apk
```

### Option C — GitHub Actions (sans Android Studio)

Créer `.github/workflows/build.yml` :
```yaml
name: Build APK
on: push
jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'temurin'
      - run: chmod +x gradlew && ./gradlew assembleDebug
      - uses: actions/upload-artifact@v4
        with:
          name: fomokiller-debug
          path: app/build/outputs/apk/debug/*.apk
```

---

## Première utilisation

1. Installer l'APK
2. Ouvrir FomoKiller
3. Une bannière apparaît → appuyer sur un bouton → aller dans **Paramètres → Accès aux notifications** → activer FomoKiller
4. Revenir dans l'app → choisir votre mode

---

## Architecture

```
FomoNotificationService   ← NotificationListenerService (noyau)
AppState                  ← Singleton + SharedPreferences (mode, listes d'apps)
MainActivity              ← UI 3 boutons
AppPickerActivity         ← Sélection des apps à bloquer/autoriser
BootReceiver              ← Restaure l'état au redémarrage
```

## Packages système toujours autorisés (mode VIP)

- Téléphone / appels entrants
- SMS natifs
- Horloge / alarmes
- SystemUI / Paramètres
