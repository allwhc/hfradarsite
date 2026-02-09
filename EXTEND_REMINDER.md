# PythonAnywhere "Run Until" Extension Guide

## The Problem

PythonAnywhere **free accounts** require you to:
- Login to the dashboard every **3 months**
- Click the **"Run until [date]"** button to extend your webapp
- This **cannot be automated** via API (by design)

## Solutions

### ✅ Solution 1: Calendar Reminder (Recommended)

Set a **recurring reminder** in your calendar:

**Setup:**
1. Open Google Calendar / Outlook / Phone Calendar
2. Create new recurring event:
   - **Title**: "Extend PythonAnywhere - hfradarsite"
   - **Frequency**: Every 2 months (to have 1 month buffer)
   - **Reminder**: 1 day before
3. Save

**When reminder triggers:**
1. Login to https://www.pythonanywhere.com
2. Go to **Web** tab
3. Click **"Run until [date]"** button
4. Done! (takes 10 seconds)

---

### ✅ Solution 2: Cron Reminder (On PythonAnywhere)

Set up a monthly notification log:

```bash
cd ~/deploy
chmod +x monthly_reminder.sh

# Add to crontab (runs 1st of every month)
crontab -e
```

Add this line:
```cron
0 9 1 * * /home/hfradarsite/deploy/monthly_reminder.sh
```

Check reminder:
```bash
cat ~/reminder.log
```

---

### ✅ Solution 3: Upgrade to Paid Plan

**PythonAnywhere Hacker Plan ($5/month):**
- ✅ No manual extension needed
- ✅ Always-on webapps
- ✅ More CPU/RAM
- ✅ SSH access
- ✅ Scheduled tasks

**Worth it if:**
- You don't want to worry about manual extension
- Your site is important for business/production
- You value peace of mind

---

### ❌ Solution 4: Browser Automation (NOT Recommended)

You could use Selenium/Playwright to automate the button click, but:
- ❌ Violates PythonAnywhere Terms of Service
- ❌ Complex to set up and maintain
- ❌ May break if UI changes
- ❌ Requires storing your password (security risk)

**Don't do this!**

---

## What Happens If You Forget?

If you don't extend within 3 months:
1. ⚠️ Your webapp gets **suspended**
2. ⚠️ Site becomes **inaccessible**
3. ✅ Your **files/database remain intact**
4. ✅ You can **re-enable** by logging in and clicking "Run until"

So it's not catastrophic - just an inconvenience.

---

## My Recommendation

**Best approach for free account:**

1. ✅ Set a **Google Calendar reminder** (every 2 months)
2. ✅ Enable **UptimeRobot monitoring** (so you know if site goes down)
3. ✅ Keep the cron reminder as backup

**Total time required:** 10 seconds every 2 months

**OR**

Upgrade to **$5/month plan** and never worry about it again!

---

## Quick Reference

### When to extend?
- **Current expiry date**: Shown on PythonAnywhere Web tab
- **Recommended**: Extend every 2 months (1 month buffer)

### How to extend?
1. Login: https://www.pythonanywhere.com
2. Web tab → Find your webapp
3. Click **"Run until [date]"** button
4. Done!

### How to check expiry date?
On PythonAnywhere Web tab, you'll see:
```
hfradarsite.pythonanywhere.com
[Configuration] [Reload] [Run until XX XXX 20XX]
                           ^^^^^^^^^^^^^^^^^^
                           This is your expiry date
```

---

## Summary

| Solution | Pros | Cons | Cost |
|----------|------|------|------|
| Calendar Reminder | Simple, reliable | Manual action needed | Free |
| Cron Reminder | Automatic log | Still manual action | Free |
| Paid Plan | Fully automatic | Costs money | $5/mo |
| Browser Automation | Automatic | Complex, risky, violates ToS | Free |

**Winner:** Calendar Reminder + Keep site alive with UptimeRobot ✅
