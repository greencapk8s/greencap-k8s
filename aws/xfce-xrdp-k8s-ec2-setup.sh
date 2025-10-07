#!/bin/bash

# Install xfce
sudo apt update
sudo apt install -y xfce4 xfce4-goodies xrdp firefox
echo xfce4-session > ~/.xsession
sudo usermod -aG ssl-cert ubuntu
sudo systemctl restart xrdp
sudo reboot

# Create password for user ubuntu.
# Example: sudo passwd <your_password>

# Test RDP connection via remote desktop.
# Recommended: Remmina.

# Install Greencap K8s.
git clone https://github.com/greencapk8s/greencap-k8s.git
cd greencap-k8s
USER_NAME_INSTALL="ubuntu" ./installers/run-installers.sh
