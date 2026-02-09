#!/bin/bash
# Keep PythonAnywhere Site Alive Script
# This script pings your website to prevent it from being suspended

# Configuration
SITE_URL="https://hfradarsite.pythonanywhere.com"
LOG_FILE="/home/hfradarsite/keep_alive.log"

# Get current timestamp
TIMESTAMP=$(date "+%Y-%m-%d %H:%M:%S")

# Ping the website
echo "[$TIMESTAMP] Pinging $SITE_URL..." >> "$LOG_FILE"

# Use curl to visit the site (silent, follow redirects, timeout 30s)
RESPONSE=$(curl -s -o /dev/null -w "%{http_code}" -L --max-time 30 "$SITE_URL")

if [ "$RESPONSE" = "200" ]; then
    echo "[$TIMESTAMP] SUCCESS: Site is alive (HTTP $RESPONSE)" >> "$LOG_FILE"
else
    echo "[$TIMESTAMP] WARNING: Received HTTP $RESPONSE" >> "$LOG_FILE"
fi

# Keep log file under 1000 lines
tail -n 1000 "$LOG_FILE" > "$LOG_FILE.tmp" && mv "$LOG_FILE.tmp" "$LOG_FILE"
