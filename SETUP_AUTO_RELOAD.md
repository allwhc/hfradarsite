# Setup Auto-Reload for GitHub Updates

This guide explains how to configure automatic web app reloading when updating from GitHub.

## Overview

When you click "Update from GitHub" in the admin panel, the system will:
1. Auto-download database backup
2. Pull latest code from GitHub
3. **Automatically reload the PythonAnywhere web app** (if configured)

## Setup Instructions

### Step 1: Get Your PythonAnywhere API Token

1. Log in to your PythonAnywhere account
2. Go to **Account** → **API Token** (https://www.pythonanywhere.com/account/#api_token)
3. Click **"Create a new API token"** if you don't have one
4. Copy the token (it looks like: `a1b2c3d4e5f6g7h8i9j0k1l2m3n4o5p6q7r8s9t0`)

### Step 2: Set Environment Variables in PythonAnywhere

Open a **Bash console** in PythonAnywhere and run these commands:

```bash
# Navigate to your app directory
cd ~/deploy

# Set the environment variables (replace with your actual values)
echo 'export PA_USERNAME="hfradarsite"' >> ~/.bashrc
echo 'export PA_DOMAIN="hfradarsite.pythonanywhere.com"' >> ~/.bashrc
echo 'export PA_API_TOKEN="YOUR_API_TOKEN_HERE"' >> ~/.bashrc

# Reload bash configuration
source ~/.bashrc
```

**Replace:**
- `YOUR_API_TOKEN_HERE` with your actual API token from Step 1
- `hfradarsite` with your PythonAnywhere username (if different)
- `hfradarsite.pythonanywhere.com` with your domain (if different)

### Step 3: Verify Configuration

Check that the variables are set:

```bash
echo $PA_USERNAME
echo $PA_DOMAIN
echo $PA_API_TOKEN
```

You should see your values printed.

### Step 4: Reload Your Web App

Go to the **Web** tab in PythonAnywhere and click the **Reload** button to apply the environment variables.

## Testing

1. Go to your admin panel → **Database Backup** tab
2. Click **"🚀 Update from GitHub"**
3. If configured correctly, you should see: **"✅ Web app reloaded automatically!"**
4. If not configured, you'll see a message to reload manually

## Security Notes

- **Keep your API token secret!** Don't share it or commit it to GitHub
- The token is stored as an environment variable (not in code)
- The `.bashrc` file is not tracked by Git

## Troubleshooting

### "API token not configured"
- Make sure you ran the commands in Step 2
- Verify with `echo $PA_API_TOKEN`
- Reload your web app after setting variables

### "Auto-reload failed (HTTP 401)"
- Your API token is invalid or expired
- Generate a new token and update the environment variable

### "Auto-reload failed (HTTP 404)"
- Check that `PA_USERNAME` and `PA_DOMAIN` are correct
- Verify with `echo $PA_USERNAME` and `echo $PA_DOMAIN`

### Manual Reload Still Works
Even if auto-reload fails, the code is still updated from GitHub. You can always reload manually from the PythonAnywhere Web tab.

## Alternative: Manual Reload

If you prefer not to use auto-reload:
- Simply don't set the environment variables
- After updating from GitHub, manually click "Reload" in PythonAnywhere Web tab
- This is perfectly fine and secure!
