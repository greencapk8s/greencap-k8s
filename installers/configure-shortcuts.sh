#!/bin/bash

set -e

mkdir -p /home/vagrant/Desktop
chown vagrant:vagrant /home/vagrant/Desktop

# [begin] Minimal setup.
touch /home/vagrant/Desktop/kube-dashboard.desktop
echo "[Desktop Entry]
Name=Kubernetes Dashboard
Exec=firefox https://kubernetes-dashboard.greencap:30002/
Icon=firefox
Terminal=false
Type=Application
Categories=Network;WebBrowser;
" > /home/vagrant/Desktop/kube-dashboard.desktop
chmod +x /home/vagrant/Desktop/kube-dashboard.desktop
chown vagrant:vagrant /home/vagrant/Desktop/kube-dashboard.desktop

touch /home/vagrant/Desktop/hello-apache.desktop
echo "[Desktop Entry]
Name=Hello Apache
Exec=firefox http://domain.local:30001/hello-apache/
Icon=firefox
Terminal=false
Type=Application
Categories=Network;WebBrowser;
" > /home/vagrant/Desktop/hello-apache.desktop
chmod +x /home/vagrant/Desktop/hello-apache.desktop
chown vagrant:vagrant /home/vagrant/Desktop/hello-apache.desktop
# [end] Minimal setup.

if [ "$MONITORING_INSTALL" = "true" ]; then
    touch /home/vagrant/Desktop/grafana.desktop
    echo "[Desktop Entry]
    Name=Grafana
    Exec=firefox http://grafana.greencap:30001/
    Icon=firefox
    Terminal=false
    Type=Application
    Categories=Network;WebBrowser;
    " > /home/vagrant/Desktop/grafana.desktop
    chmod +x /home/vagrant/Desktop/grafana.desktop
    chown vagrant:vagrant /home/vagrant/Desktop/grafana.desktop
fi
