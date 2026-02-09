# HF Radar Android App - Source Files

This folder contains all source files needed to rebuild the HF Radar Android mobile app.

## 📱 App Details

- **Package Name**: `com.fieldsync.app`
- **App ID**: `com.example.hfradar`
- **Min SDK**: Android 7.0 (API 24)
- **Target SDK**: Android 14 (API 34)
- **Default Server**: https://hfradarsite.pythonanywhere.com

## 🔧 Prerequisites

To build the app, you need:
1. **Java JDK 8 or higher**
2. **Android SDK** (automatically downloaded by Gradle if not present)
3. **Git** (to clone this repository)

## 📂 Folder Structure

```
android-app/
├── src/                    # Complete app source code
│   └── main/
│       ├── java/           # Java source files
│       ├── res/            # Resources (layouts, images, etc.)
│       └── AndroidManifest.xml
├── gradle/                 # Gradle wrapper files
├── build.gradle            # App-level build configuration
├── build.gradle.root       # Project-level build configuration
├── settings.gradle         # Project settings
├── gradle.properties       # Gradle properties
├── gradlew                 # Gradle wrapper (Linux/Mac)
├── gradlew.bat             # Gradle wrapper (Windows)
└── README.md               # This file
```

## 🚀 How to Build the APK

### Option 1: Build on Windows

1. **Clone the repository** (if not already cloned):
   ```bash
   git clone https://github.com/allwhc/hfradarsite.git
   cd hfradarsite/android-app
   ```

2. **Create project structure**:
   ```bash
   # Create app folder
   mkdir app

   # Move source files
   move src app\src
   move build.gradle app\build.gradle

   # Rename root build.gradle
   move build.gradle.root build.gradle
   ```

3. **Build the APK**:
   ```bash
   gradlew.bat assembleDebug
   ```

4. **Find your APK**:
   ```
   app\build\outputs\apk\debug\app-debug.apk
   ```

### Option 2: Build on Linux/Mac

1. **Clone the repository**:
   ```bash
   git clone https://github.com/allwhc/hfradarsite.git
   cd hfradarsite/android-app
   ```

2. **Create project structure**:
   ```bash
   # Create app folder
   mkdir app

   # Move source files
   mv src app/src
   mv build.gradle app/build.gradle

   # Rename root build.gradle
   mv build.gradle.root build.gradle

   # Make gradlew executable
   chmod +x gradlew
   ```

3. **Build the APK**:
   ```bash
   ./gradlew assembleDebug
   ```

4. **Find your APK**:
   ```
   app/build/outputs/apk/debug/app-debug.apk
   ```

### Option 3: Quick Setup Script (Windows)

Save this as `setup-and-build.bat` in the `android-app` folder:

```batch
@echo off
echo Setting up Android project structure...

mkdir app 2>nul
move src app\src
move build.gradle app\build.gradle
move build.gradle.root build.gradle

echo Building APK...
call gradlew.bat assembleDebug

echo.
echo Build complete! APK location:
echo app\build\outputs\apk\debug\app-debug.apk
pause
```

Then just run:
```bash
setup-and-build.bat
```

### Option 4: Quick Setup Script (Linux/Mac)

Save this as `setup-and-build.sh` in the `android-app` folder:

```bash
#!/bin/bash
echo "Setting up Android project structure..."

mkdir -p app
mv src app/src
mv build.gradle app/build.gradle
mv build.gradle.root build.gradle

chmod +x gradlew

echo "Building APK..."
./gradlew assembleDebug

echo ""
echo "Build complete! APK location:"
echo "app/build/outputs/apk/debug/app-debug.apk"
```

Then run:
```bash
chmod +x setup-and-build.sh
./setup-and-build.sh
```

## 📝 Key Features

### 1. Notification Permission Request
- **Automatically requests** notification permission on first app launch
- Required for monthly reminder notifications (Android 13+)
- Only asks once (won't annoy users repeatedly)

### 2. Monthly Reminder Notifications
- Reminds users to upload data every month
- Uses WorkManager for reliable scheduling
- Survives device reboots

### 3. Default Server Configuration
- Pre-configured to point to: `https://hfradarsite.pythonanywhere.com`
- Users can change server URL in Settings if needed

### 4. Data Management
- QR code scanning for site verification
- TSU data upload to server
- Data history tracking
- Backup and restore functionality

## 🔐 App Permissions

The app requires these permissions (declared in AndroidManifest.xml):

- `CAMERA` - For QR code scanning
- `BLUETOOTH` - For device communication
- `ACCESS_COARSE_LOCATION` - For location-based features
- `INTERNET` - For server communication
- `POST_NOTIFICATIONS` - For monthly reminders (Android 13+)
- `RECEIVE_BOOT_COMPLETED` - To reschedule reminders after device restart

## 🛠️ Customization

### Change Default Server URL

Edit these files:

1. **VerifyDataFragment.java** (Line 212):
   ```java
   String baseUrl = prefs.getString("serv_url", "YOUR_SERVER_URL");
   ```

2. **ExportTsuData.java** (Line 589):
   ```java
   String baseUrl = settings1.getString("serv_url", "YOUR_SERVER_URL");
   ```

Then rebuild the APK.

### Change App Password

Default password is `test`. To change it, users can:
1. Open app Settings/Configuration
2. Update "App Password" field
3. Save changes

Or you can modify the default in `login_form.java`:
```java
String pwd = "your_new_default_password";
```

### Change Package Name

If you need to change the package name (to avoid conflicts):

1. Update `build.gradle` (app-level):
   ```gradle
   defaultConfig {
       applicationId "com.yourcompany.yourapp"
   }
   ```

2. Refactor package in source code (use Android Studio for this)

## 📦 Dependencies

The app uses these key libraries:

- **AndroidX** - Modern Android components
- **Material Design** - UI components
- **WorkManager** - Background task scheduling
- **Google Play Services Vision** - QR code scanning
- **Upload Service** - File upload functionality

All dependencies are automatically downloaded during build.

## 🐛 Troubleshooting

### Build fails with "SDK not found"
- Gradle will auto-download Android SDK on first build
- Make sure you have internet connection
- Ensure Java JDK is installed

### Permission denied on gradlew (Linux/Mac)
```bash
chmod +x gradlew
```

### Out of memory during build
Add to `gradle.properties`:
```properties
org.gradle.jvmargs=-Xmx2048m
```

### Build is slow
- First build takes 5-10 minutes (downloads dependencies)
- Subsequent builds are much faster (15-30 seconds)
- Enable Gradle daemon in `gradle.properties`:
  ```properties
  org.gradle.daemon=true
  ```

## 📱 Installing the APK

### On Physical Device:
1. Copy `app-debug.apk` to your phone
2. Open the file
3. Allow "Install from Unknown Sources" if prompted
4. Click Install

### Via ADB:
```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

## 🔄 Version History

- **v1.0** (Feb 2026)
  - Initial release with notification permission request
  - Monthly reminder notifications
  - Default server: hfradarsite.pythonanywhere.com
  - QR code scanning and data upload

## 📞 Support

For issues or questions:
- Open an issue on GitHub
- Contact: Lab_Engineer

## 📄 License

This is proprietary software for HF Radar data collection and management.
