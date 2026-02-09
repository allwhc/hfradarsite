# HF Radar Repository Structure

This repository contains both the **Flask web server** and **Android mobile app** source code.

## 📁 Repository Organization

```
hfradarsite/
├── android-app/                    # Android mobile app source files
│   ├── src/                        # Complete app source code
│   │   └── main/
│   │       ├── java/              # Java source files
│   │       ├── res/               # Resources (layouts, images, etc.)
│   │       └── AndroidManifest.xml
│   ├── prebuilt/                  # Pre-built APK files
│   │   ├── app-debug-latest.apk   # Latest debug APK
│   │   └── README.md
│   ├── gradle/                    # Gradle wrapper
│   ├── build.gradle               # App build config
│   ├── build.gradle.root          # Root build config
│   ├── settings.gradle            # Project settings
│   ├── gradle.properties          # Gradle properties
│   ├── gradlew                    # Gradle wrapper (Unix)
│   ├── gradlew.bat                # Gradle wrapper (Windows)
│   ├── setup-and-build.sh         # Quick build script (Unix)
│   ├── setup-and-build.bat        # Quick build script (Windows)
│   └── README.md                  # Build instructions
│
├── static/                        # Web server static files
│   ├── css/                       # Stylesheets
│   ├── js/                        # JavaScript files
│   │   └── configangularadm.js   # Admin UI logic
│   └── img/                       # Images
│
├── templates/                     # Flask HTML templates
│   ├── admhome.html              # Admin dashboard
│   └── ...
│
├── app.py                        # Main Flask application
├── users.db                      # SQLite database (preserved by git stash)
├── wsgi.py                       # WSGI configuration
│
├── SETUP_AUTO_RELOAD.md          # PythonAnywhere API setup guide
├── WSGI_SETUP.md                 # WSGI configuration guide
├── QUICK_START.md                # Quick reference
├── KEEP_ALIVE_SETUP.md           # Keep site alive setup
├── EXTEND_REMINDER.md            # "Run until" button guide
├── keep_alive.sh                 # Site ping script
├── monthly_reminder.sh           # Monthly reminder script
└── REPOSITORY_STRUCTURE.md       # This file
```

## 🚀 Quick Start

### Deploy Web Server (PythonAnywhere)

1. **Clone the repository**:
   ```bash
   git clone https://github.com/allwhc/hfradarsite.git
   cd hfradarsite
   ```

2. **Configure WSGI** (see [WSGI_SETUP.md](WSGI_SETUP.md)):
   - Set environment variables for PythonAnywhere API
   - Configure Flask app path

3. **Reload the web app**:
   ```bash
   # Via PythonAnywhere dashboard
   # OR use the admin UI "Update from GitHub" button
   ```

### Build Android App

1. **Clone the repository**:
   ```bash
   git clone https://github.com/allwhc/hfradarsite.git
   cd hfradarsite/android-app
   ```

2. **Run build script**:

   **Windows**:
   ```bash
   setup-and-build.bat
   ```

   **Linux/Mac**:
   ```bash
   chmod +x setup-and-build.sh
   ./setup-and-build.sh
   ```

3. **Find your APK**:
   ```
   app/build/outputs/apk/debug/app-debug.apk
   ```

### Or Use Pre-built APK

Download the latest pre-built APK:
```
android-app/prebuilt/app-debug-latest.apk
```

## 🔄 Update Workflow

### Update Web Server from GitHub

**Option 1: Via Admin UI** (Recommended)
1. Login to web admin: `https://hfradarsite.pythonanywhere.com/admin`
2. Go to "Backup & Updates" tab
3. Click "Update from GitHub"
4. Wait for reload to complete (~30 seconds)

**Option 2: Via PythonAnywhere Bash**
```bash
cd ~/hfradarsite
git stash push -u users.db    # Preserve database
git pull origin main           # Get latest code
git stash pop                  # Restore database
# Then reload web app via dashboard
```

### Rebuild Mobile App

When you need to rebuild the app (e.g., after changing server URL):

1. Pull latest code:
   ```bash
   cd ~/hfradarsite/android-app
   git pull origin main
   ```

2. Rebuild:
   ```bash
   # Windows
   setup-and-build.bat

   # Linux/Mac
   ./setup-and-build.sh
   ```

## 📦 What's Included

### Web Server Features
- ✅ Flask REST API for mobile app
- ✅ Admin dashboard for data management
- ✅ User authentication
- ✅ SQLite database
- ✅ Database backup/restore
- ✅ One-click GitHub updates with auto-reload
- ✅ Monthly reminder scheduling

### Mobile App Features
- ✅ Notification permission request on first launch
- ✅ Monthly reminder notifications
- ✅ QR code scanning
- ✅ TSU data upload to server
- ✅ Data history tracking
- ✅ Configurable server URL
- ✅ Default server: hfradarsite.pythonanywhere.com

## 🔐 Security Notes

### API Tokens
- PythonAnywhere API token is stored in WSGI file (environment variables)
- Never commit tokens to public repositories
- Current token is for auto-reload functionality

### Database
- `users.db` is excluded from git (via git stash during updates)
- Always backup database before major changes
- Use admin UI "Download Database Backup" feature

## 📞 Support

**GitHub Repository**: https://github.com/allwhc/hfradarsite

**For issues**:
1. Check documentation in this repo
2. Review setup guides (SETUP_AUTO_RELOAD.md, WSGI_SETUP.md)
3. Open an issue on GitHub

## 📝 Version Info

**Last Updated**: February 9, 2026

**Current Versions**:
- Web Server: v1.0
- Android App: v1.0 (with notification permission)

**Repository**: https://github.com/allwhc/hfradarsite
