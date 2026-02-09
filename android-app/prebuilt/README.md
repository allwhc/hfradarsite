# Pre-built APK

This folder contains the latest pre-built APK for quick deployment.

## 📱 Latest Build

**File**: `app-debug-latest.apk`

**Build Details**:
- **Build Date**: February 9, 2026
- **Version**: 1.0
- **Package**: com.fieldsync.app
- **Min Android**: 7.0 (API 24)
- **Target Android**: 14 (API 34)
- **Features**:
  - ✅ Notification permission request on first launch
  - ✅ Monthly reminder notifications
  - ✅ Default server: https://hfradarsite.pythonanywhere.com
  - ✅ QR code scanning
  - ✅ Data upload and sync

## 🚀 Quick Install

### Method 1: Direct Install on Device
1. Copy `app-debug-latest.apk` to your Android device
2. Open the file on your device
3. Allow "Install from Unknown Sources" if prompted
4. Tap "Install"

### Method 2: Install via ADB
```bash
adb install app-debug-latest.apk
```

### Method 3: Install via Email/Cloud
1. Email the APK to yourself or upload to Google Drive/Dropbox
2. Open on your Android device
3. Download and install

## 🔄 When to Rebuild

You should rebuild the APK from source if you need to:
- Change the default server URL
- Modify app features or UI
- Update dependencies
- Change app password or credentials
- Customize branding

To rebuild, see the main [README.md](../README.md) in the parent folder.

## ⚠️ Important Notes

- This is a **debug APK** (not signed for Play Store)
- Only for internal use and testing
- To publish on Play Store, you need to create a signed release APK
- Users may need to enable "Install from Unknown Sources" in Android settings

## 📱 System Requirements

- **Android Version**: 7.0 or higher
- **Storage**: ~10 MB required
- **Permissions**: Camera, Bluetooth, Location, Notifications
