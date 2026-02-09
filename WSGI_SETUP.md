# PythonAnywhere WSGI Configuration for Environment Variables

## The Problem

Environment variables set in `~/.bashrc` are NOT automatically available to PythonAnywhere web apps.
You need to set them in the WSGI configuration file.

## Solution: Update WSGI File

### Step 1: Open WSGI Configuration

1. Go to PythonAnywhere → **Web** tab
2. Scroll down to **"Code"** section
3. Click on the **WSGI configuration file** link (e.g., `/var/www/hfradarsite_pythonanywhere_com_wsgi.py`)

### Step 2: Add Environment Variables

Add these lines **at the very top** of the WSGI file (before any imports):

```python
import os
import sys

# Set environment variables for PythonAnywhere API
os.environ['PA_USERNAME'] = 'hfradarsite'
os.environ['PA_DOMAIN'] = 'hfradarsite.pythonanywhere.com'
os.environ['PA_API_TOKEN'] = '052147026d092fba5b7c51ef5c673fe568283f4c'

# Add your project directory to the sys.path
path = '/home/hfradarsite/deploy'
if path not in sys.path:
    sys.path.append(path)

# Import Flask app
from app import app as application
```

### Step 3: Save and Reload

1. Click **"Save"** button (top right)
2. Go back to **Web** tab
3. Click **"Reload hfradarsite.pythonanywhere.com"** button

### Step 4: Test

1. Go to your admin panel
2. Click **Database Backup** tab
3. Click **"🚀 Update from GitHub"**
4. After pulling code, click **"Reload Web App"**
5. Should now see: **"✅ Web app reloaded successfully!"**

---

## Full WSGI File Example

Here's what your complete WSGI file should look like:

```python
import os
import sys

# =============================================================
# ENVIRONMENT VARIABLES FOR AUTO-RELOAD
# =============================================================
os.environ['PA_USERNAME'] = 'hfradarsite'
os.environ['PA_DOMAIN'] = 'hfradarsite.pythonanywhere.com'
os.environ['PA_API_TOKEN'] = '052147026d092fba5b7c51ef5c673fe568283f4c'

# =============================================================
# PROJECT PATH
# =============================================================
path = '/home/hfradarsite/deploy'
if path not in sys.path:
    sys.path.append(path)

# =============================================================
# FLASK APP
# =============================================================
from app import app as application
```

---

## Why This Works

- WSGI file is executed **before** Flask app starts
- Environment variables set here are available to `app.py`
- `os.environ.get('PA_API_TOKEN', '')` in app.py will now find the token
- Auto-reload feature will work! ✅

---

## Verification

To verify it's working, add a debug endpoint in your app.py (temporary, for testing):

```python
@app.route("/admin/checkEnv", methods=['GET'])
def checkEnv():
    return json.dumps({
        'PA_USERNAME': os.environ.get('PA_USERNAME', 'NOT SET'),
        'PA_DOMAIN': os.environ.get('PA_DOMAIN', 'NOT SET'),
        'PA_API_TOKEN': os.environ.get('PA_API_TOKEN', 'NOT SET')[:20] + '...'  # Only show first 20 chars
    })
```

Then visit: `https://hfradarsite.pythonanywhere.com/admin/checkEnv`

You should see your values instead of "NOT SET".
