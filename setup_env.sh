#!/bin/bash
# PythonAnywhere Environment Setup Script
# Run this in PythonAnywhere Bash console

echo "Setting up PythonAnywhere environment variables..."

# Set environment variables
export PA_USERNAME="hfradarsite"
export PA_DOMAIN="hfradarsite.pythonanywhere.com"
export PA_API_TOKEN="052147026d092fba5b7c51ef5c673fe568283f4c"

# Add to .bashrc for persistence
echo 'export PA_USERNAME="hfradarsite"' >> ~/.bashrc
echo 'export PA_DOMAIN="hfradarsite.pythonanywhere.com"' >> ~/.bashrc
echo 'export PA_API_TOKEN="052147026d092fba5b7c51ef5c673fe568283f4c"' >> ~/.bashrc

echo ""
echo "✓ Environment variables set!"
echo ""
echo "Current values:"
echo "  PA_USERNAME: $PA_USERNAME"
echo "  PA_DOMAIN: $PA_DOMAIN"
echo "  PA_API_TOKEN: ${PA_API_TOKEN:0:20}..." # Show only first 20 chars
echo ""
echo "Next steps:"
echo "1. Go to PythonAnywhere Web tab"
echo "2. Click the 'Reload' button for your web app"
echo "3. Test the auto-reload feature from admin panel"
echo ""
