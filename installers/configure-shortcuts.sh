#!/bin/bash

set -e

mkdir -p /home/$USER_NAME_INSTALL/Desktop
chown $USER_NAME_INSTALL:$USER_NAME_INSTALL /home/$USER_NAME_INSTALL/Desktop

# [begin] Minimal setup.
touch /home/$USER_NAME_INSTALL/Desktop/kube-dashboard.desktop
echo "[Desktop Entry]
Name=Kubernetes Dashboard
Exec=firefox https://kubernetes-dashboard.greencap:30002/
Icon=firefox
Terminal=false
Type=Application
Categories=Network;WebBrowser;
" > /home/$USER_NAME_INSTALL/Desktop/kube-dashboard.desktop
chmod +x /home/vagrant/Desktop/kube-dashboard.desktop
chown $USER_NAME_INSTALL:$USER_NAME_INSTALL /home/$USER_NAME_INSTALL/Desktop/kube-dashboard.desktop

touch /home/$USER_NAME_INSTALL/Desktop/hello-apache.desktop
echo "[Desktop Entry]
Name=Hello Apache
Exec=firefox http://domain.local:30001/hello-apache/
Icon=firefox
Terminal=false
Type=Application
Categories=Network;WebBrowser;
" > /home/vagrant/Desktop/hello-apache.desktop
chmod +x /home/$USER_NAME_INSTALL/Desktop/hello-apache.desktop
chown $USER_NAME_INSTALL:$USER_NAME_INSTALL /home/$USER_NAME_INSTALL/Desktop/hello-apache.desktop
# [end] Minimal setup.

if [[ "$MONITORING_INSTALL" == "true" ]]; then
    touch /home/$USER_NAME_INSTALL/Desktop/grafana.desktop
    echo "[Desktop Entry]
    Name=Grafana
    Exec=firefox http://grafana.greencap:30001/
    Icon=firefox
    Terminal=false
    Type=Application
    Categories=Network;WebBrowser;
    " > /home/$USER_NAME_INSTALL/Desktop/grafana.desktop
    chmod +x /home/$USER_NAME_INSTALL/Desktop/grafana.desktop
    chown $USER_NAME_INSTALL:$USER_NAME_INSTALL /home/$USER_NAME_INSTALL/Desktop/grafana.desktop
fi
