#!/bin/bash
# Auto-extend PythonAnywhere Web App
# Attempts to extend the "Run until" date via API

API_TOKEN="052147026d092fba5b7c51ef5c673fe568283f4c"
USERNAME="hfradarsite"
DOMAIN="hfradarsite.pythonanywhere.com"

# Try to get current webapp info
echo "Checking webapp status..."
curl -s -H "Authorization: Token $API_TOKEN" \
  "https://www.pythonanywhere.com/api/v0/user/$USERNAME/webapps/$DOMAIN/" | python3 -m json.tool

# Note: PythonAnywhere API doesn't have an endpoint to extend the expiry date
# This is intentional for free accounts - you must login manually
echo ""
echo "WARNING: PythonAnywhere free tier requires manual extension"
echo "You must login to dashboard and click 'Run until' button"
