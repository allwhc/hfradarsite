# Quick Start Guide - Auto-Reload Setup

## ⚡ Fast Setup (2 minutes)

### Option 1: Automated Setup (Recommended)

In PythonAnywhere Bash console:

```bash
cd ~/deploy
bash setup_env.sh
```

Then reload your web app from the Web tab.

---

### Option 2: Manual Setup

In PythonAnywhere Bash console:

```bash
cd ~/deploy

# Set environment variables
echo 'export PA_USERNAME="hfradarsite"' >> ~/.bashrc
echo 'export PA_DOMAIN="hfradarsite.pythonanywhere.com"' >> ~/.bashrc
echo 'export PA_API_TOKEN="052147026d092fba5b7c51ef5c673fe568283f4c"' >> ~/.bashrc

# Reload bash configuration
source ~/.bashrc

# Verify
echo $PA_API_TOKEN
```

Then reload your web app from the Web tab.

---

## ✅ Testing

1. Go to your site: https://hfradarsite.pythonanywhere.com
2. Login as admin
3. Navigate to **Database Backup** tab
4. Click **"🚀 Update from GitHub"**
5. Follow the workflow:
   - Backup downloads automatically
   - Code pulls from GitHub (database preserved)
   - Restore the backup from UI
   - Click **"Reload Web App"** button
   - Should see: **"✅ Web app reloaded successfully!"**

---

## 🔧 How It Works

The auto-reload uses PythonAnywhere's API:

```
POST https://www.pythonanywhere.com/api/v0/user/hfradarsite/webapps/hfradarsite.pythonanywhere.com/reload/
Authorization: Token 052147026d092fba5b7c51ef5c673fe568283f4c
```

**Environment variables loaded from ~/.bashrc:**
- `PA_USERNAME` → Your PythonAnywhere username
- `PA_DOMAIN` → Your web app domain
- `PA_API_TOKEN` → Your API token for authentication

---

## 🔒 Security Note

The API token is stored in `~/.bashrc` which is:
- ✅ Not tracked by Git
- ✅ Only accessible to your PythonAnywhere account
- ✅ Loaded automatically when Flask app starts

**Optional:** After setup, you can regenerate the token on PythonAnywhere (Account → API Token) for extra security.

---

## 🎯 What Happens During Update

1. **Backup** → Auto-downloads database JSON to your computer
2. **Git Stash** → Preserves current `users.db` file
3. **Git Pull** → Updates code from GitHub
4. **Git Stash Pop** → Restores current `users.db` file
5. **Manual Restore** → You restore backup from UI (ensures data consistency)
6. **Auto-Reload** → Web app reloads via API (if token configured)

**Result:** Latest code + Your current data = ✅ Safe update!
