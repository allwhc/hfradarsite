#!/bin/bash
# Monthly Reminder to Extend PythonAnywhere
# This script sends you a reminder email/notification

LOG_FILE="/home/hfradarsite/reminder.log"
TIMESTAMP=$(date "+%Y-%m-%d %H:%M:%S")

echo "[$TIMESTAMP] Monthly reminder: Please extend PythonAnywhere webapp" >> "$LOG_FILE"

# You can add email notification here if you have email configured
# Or just check the log file monthly

cat << EOF
========================================
REMINDER: Extend PythonAnywhere Webapp
========================================

Date: $TIMESTAMP

ACTION REQUIRED:
1. Login to https://www.pythonanywhere.com
2. Go to Web tab
3. Click "Run until [date]" button for hfradarsite.pythonanywhere.com

This ensures your site stays active for another 3 months.

========================================
EOF
