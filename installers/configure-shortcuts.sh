#!/bin/bash

set -e

SUPPORTED_USERS=("vagrant" "ubuntu")
if [[ ! " ${SUPPORTED_USERS[@]} " =~ " ${USER_NAME_INSTALL} " ]]; then
    echo "Skipping shortcut configuration for user: $USER_NAME_INSTALL"
    exit 0
fi

mkdir -p /home/$USER_NAME_INSTALL/Desktop
chown $USER_NAME_INSTALL:$USER_NAME_INSTALL /home/$USER_NAME_INSTALL/Desktop

touch /home/$USER_NAME_INSTALL/Desktop/greencap-tech-docs.desktop
echo "[Desktop Entry]
Name=GreenCap Tech Docs
Exec=firefox http://tech-docs.greencap:30001/
Icon=firefox
Terminal=false
Type=Application
Categories=Network;WebBrowser;
" > /home/$USER_NAME_INSTALL/Desktop/greencap-tech-docs.desktop
chmod +x /home/$USER_NAME_INSTALL/Desktop/greencap-tech-docs.desktop
chown $USER_NAME_INSTALL:$USER_NAME_INSTALL /home/$USER_NAME_INSTALL/Desktop/greencap-tech-docs.desktop

echo ""
echo "=========================================="
echo "✅ Shortcuts configured successfully!"
echo "=========================================="
