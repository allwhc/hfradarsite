# Keep PythonAnywhere Site Alive - Auto Setup

## Problem
PythonAnywhere free tier suspends sites after **3 months of inactivity**. You need to visit the site or click "Run until 3 months" button to keep it active.

## Solution
Set up a **cron job** that automatically visits your site every month to keep it alive.

---

## Setup Instructions

### Step 1: Upload the Keep-Alive Script

On PythonAnywhere, the script is already in your repo. Just make it executable:

```bash
cd ~/deploy
chmod +x keep_alive.sh
```

### Step 2: Set Up Cron Job

1. Go to PythonAnywhere → **Tasks** tab
2. Click **"Create a new scheduled task"**
3. Configure:
   - **Frequency**: Daily at 00:00 (or any time you prefer)
   - **Command**: `/home/hfradarsite/deploy/keep_alive.sh`

**OR** manually edit crontab:

```bash
crontab -e
```

Add this line (runs daily at midnight):

```cron
0 0 * * * /home/hfradarsite/deploy/keep_alive.sh
```

To run **once per week** (Monday at midnight):

```cron
0 0 * * 1 /home/hfradarsite/deploy/keep_alive.sh
```

To run **once per month** (1st day at midnight):

```cron
0 0 1 * * /home/hfradarsite/deploy/keep_alive.sh
```

### Step 3: Test the Script

Run it manually first to make sure it works:

```bash
cd ~/deploy
./keep_alive.sh
cat ~/keep_alive.log
```

You should see:
```
[2026-02-09 05:30:00] Pinging https://hfradarsite.pythonanywhere.com...
[2026-02-09 05:30:01] SUCCESS: Site is alive (HTTP 200)
```

---

## Alternative Solutions

### Option 2: External Monitoring Service (Free)

Use a free uptime monitoring service that pings your site automatically:

#### **UptimeRobot** (Recommended - 100% Free)
- Website: https://uptimerobot.com
- **Setup:**
  1. Sign up (free account)
  2. Add new monitor:
     - Monitor Type: HTTP(s)
     - URL: `https://hfradarsite.pythonanywhere.com`
     - Check interval: Every 5 minutes (free tier)
  3. Done! It will ping your site automatically

#### **Other Free Services:**
- **Pingdom** - https://pingdom.com (free tier available)
- **StatusCake** - https://statuscake.com (free tier)
- **Freshping** - https://freshping.io (free, unlimited monitors)

### Option 3: GitHub Actions (Free)

Create a GitHub Actions workflow that pings your site weekly:

Create `.github/workflows/keep-alive.yml` in your repo:

```yaml
name: Keep Site Alive

on:
  schedule:
    - cron: '0 0 * * 1'  # Every Monday at midnight UTC
  workflow_dispatch:  # Allow manual trigger

jobs:
  ping-site:
    runs-on: ubuntu-latest
    steps:
      - name: Ping website
        run: |
          curl -I https://hfradarsite.pythonanywhere.com
          echo "Site pinged successfully at $(date)"
```

---

## Recommendation

**Best approach:**

1. **Primary**: Use **UptimeRobot** (external monitoring) - 100% reliable, zero setup on PythonAnywhere
2. **Backup**: Set up cron job on PythonAnywhere (in case external monitoring fails)

This dual approach ensures your site **never** gets suspended!

---

## Monitoring & Logs

### Check if cron is running:

```bash
# View cron jobs
crontab -l

# Check logs
cat ~/keep_alive.log

# Check last 10 pings
tail -n 20 ~/keep_alive.log
```

### Expected log output:

```
[2026-02-09 00:00:01] Pinging https://hfradarsite.pythonanywhere.com...
[2026-02-09 00:00:02] SUCCESS: Site is alive (HTTP 200)
[2026-02-10 00:00:01] Pinging https://hfradarsite.pythonanywhere.com...
[2026-02-10 00:00:02] SUCCESS: Site is alive (HTTP 200)
```

---

## PythonAnywhere "Run Until" Button

Even with auto-ping, you should still visit PythonAnywhere dashboard **once every 3 months** and click the **"Run until [date]"** button to be safe.

The auto-ping keeps the **site** active, but PythonAnywhere may still track dashboard login activity separately.

---

## FAQ

**Q: Will this cost anything?**
A: No! All solutions are completely free.

**Q: How often should I ping?**
A: Once per month is enough. Daily is better for redundancy.

**Q: Can I use multiple methods?**
A: Yes! Use both UptimeRobot + cron for maximum reliability.

**Q: Will this keep my account active?**
A: It keeps your **website** from being suspended due to inactivity. You should still log into PythonAnywhere dashboard once every 3 months to be completely safe.

---

## Summary

✅ **Easiest**: Sign up for UptimeRobot (5 minutes, no coding)
✅ **Most Reliable**: UptimeRobot + PythonAnywhere Cron (dual protection)
✅ **Zero Cost**: All solutions are 100% free forever
