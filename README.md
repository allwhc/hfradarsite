# HF Radar Server - PythonAnywhere Deployment

Flask web application for HF Radar data management and reporting.

## File Structure

```
pythonanywhere-deploy/
├── app.py                 # Main Flask application
├── gsheetRAD.py           # Google Sheets integration
├── requirements.txt       # Python dependencies
├── .gitignore             # Git ignore rules
├── README.md              # This file
├── templates/             # HTML templates
│   ├── gvmlogin.html      # Login page
│   ├── TsuHome.html       # User home page
│   ├── admhome.html       # Admin home page
│   └── AboutUs.html       # About page
└── static/                # Static assets
    ├── css/
    │   ├── bootstrap.css
    │   ├── jquery-ui.css
    │   └── serverstyles.css
    ├── js/
    │   ├── angular.min.js
    │   ├── jquery-1.11.2.js
    │   ├── jquery-ui.js
    │   ├── jquery.min.js
    │   ├── configangular.js      # User frontend logic
    │   └── configangularadm.js   # Admin frontend logic
    └── images/
        └── ic_tsu.png
```

## PythonAnywhere Setup

### 1. Create GitHub Repository
1. Create a new GitHub repository (e.g., `hfradar-server`)
2. Push this folder to the repository:
   ```bash
   git init
   git add .
   git commit -m "Initial commit"
   git remote add origin https://github.com/YOUR_USERNAME/hfradar-server.git
   git push -u origin main
   ```

### 2. Clone on PythonAnywhere
1. Open a Bash console on PythonAnywhere
2. Clone your repository:
   ```bash
   cd /home/hfradarsite
   git clone https://github.com/YOUR_USERNAME/hfradar-server.git deploy
   ```

### 3. Install Dependencies
```bash
pip3 install --user -r /home/hfradarsite/deploy/requirements.txt
```

### 4. Configure Web App
1. Go to **Web** tab on PythonAnywhere
2. Set **Source code**: `/home/hfradarsite/deploy`
3. Set **Working directory**: `/home/hfradarsite/deploy`
4. Edit **WSGI configuration file**:
   ```python
   import sys
   path = '/home/hfradarsite/deploy'
   if path not in sys.path:
       sys.path.append(path)
   from app import app as application
   ```

### 5. Static Files Mapping
In the **Web** tab, add static files:
| URL | Directory |
|-----|-----------|
| /static | /home/hfradarsite/deploy/static |

### 6. Required Files (NOT in Git)
Upload these files manually to `/home/hfradarsite/deploy/`:
- `users.db` - SQLite database
- `bigbask-395319-717a61a56bff.json` - Google Sheets credentials

### 7. Update from GitHub
When you push changes to GitHub:
```bash
cd /home/hfradarsite/deploy
git pull origin main
```
Then reload the web app from the **Web** tab.

## API Endpoints

### User Endpoints
- `POST /authen` - Login authentication
- `POST /getalldatabymon` - Get monthly report data
- `POST /getalldatabyyr` - Get yearly report (percentage)
- `POST /getalldatabyyr1` - Get yearly report (totals)
- `POST /getalldatabyyr2` - Get custom date range report
- `POST /getallyearlydata` - Get annual summary data
- `POST /exportMonthlyData` - Export data to external system
- `POST /upldTsuData` - Upload radial count data

### Admin Endpoints
- `POST /admin/getSites` - Get all sites
- `POST /admin/addSite` - Add new site
- `POST /admin/editSite` - Edit site name
- `POST /admin/toggleSite` - Activate/deactivate site
- `POST /admin/reorderSites` - Reorder sites
- `POST /admin/getSitePairs` - Get site groups
- `POST /admin/addSitePair` - Add site to group
- `POST /admin/addNewGroup` - Create new group
- `POST /admin/removeSitePair` - Remove site from group
- `POST /admin/deleteGroup` - Delete group
- `POST /admin/downloadBackup` - Download config backup
- `POST /admin/restoreBackup` - Restore from backup

### Mobile App Endpoints
- `GET/POST /getConfig` - Get sites list for mobile app

## Mobile App Integration

The mobile app fetches the sites list from the server using `/getConfig` endpoint.

To update sites on mobile devices:
1. Admin adds/edits sites via admin panel
2. Mobile app users tap "Update Site List from Server" in Configuration screen
3. App fetches latest sites from `/getConfig` endpoint

## Database Schema

### Tables
- `user` - Login credentials
- `maintsu` - Daily radial count data
- `perctsu` - Monthly percentage data
- `sites` - Site list (dynamic)
- `site_pairs` - Site groupings for vector calculations
- `hfena` - Site activation status
- `data_source_log` - Track data upload sources
- `data_comments` - Day-level comments/reasons
