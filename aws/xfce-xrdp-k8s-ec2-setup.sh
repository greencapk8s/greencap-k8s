#!/bin/bash

# Install xfce
sudo apt update && sudo apt upgrade -y
sudo apt install -y xfce4 xfce4-goodies xrdp firefox
echo xfce4-session > ~/.xsession
sudo usermod -aG ssl-cert $USER
sudo systemctl restart xrdp
sudo reboot

# Create password for user ubuntu.
# Test RDP connection via remote desktop(Remmina).

# Setup python and project virtual environment.
sudo DEBIAN_FRONTEND=noninteractive apt install -y python3-pip python3-venv
sudo apt install -y apt-transport-https
git clone https://github.com/greencapk8s/greencap-k8s.git
mv greencap-k8s greencap
cd greencap
git checkout developer
cd ..
python3 -m venv ./greencap/.venv
sudo chown -R ubuntu:ubuntu ./greencap/.venv/
source ./greencap/.venv/bin/activate
pip3 install -r ./greencap/installers/requirements.txt

# Fifth step:
sudo python3 ./greencap/installers/installer.py --script "./greencap/installers/configure-docker-daemon.sh" --verbose
sudo python3 ./greencap/installers/installer.py --script "./greencap/installers/configure-hosts.sh" --verbose
sudo python3 ./greencap/installers/installer.py --script "./greencap/installers/kind-ec2-install.sh" --verbose
sudo python3 ./greencap/installers/installer.py --script "./greencap/installers/kubectl-ec2-install.sh" --verbose
sudo python3 ./greencap/installers/installer.py --script "./greencap/installers/helm-install.sh" --verbose
sudo python3 ./greencap/installers/installer.py --script "./greencap/installers/cilium-install.sh" --verbose
sudo python3 ./greencap/installers/installer.py --script "./greencap/installers/ingress-controller-install.sh" --verbose
sudo python3 ./greencap/installers/installer.py --script "./greencap/installers/apache-hello-install.sh" --verbose
sudo python3 ./greencap/installers/installer.py --script "./greencap/installers/kube-dash-install.sh" --verbose
sudo python3 ./greencap/installers/installer.py --script "./greencap/installers/harbor-install.sh" --verbose
sudo python3 ./greencap/installers/installer.py --script "./greencap/installers/postgres-install.sh" --verbose
sudo python3 ./greencap/installers/installer.py --script "./greencap/installers/ecom-python-install.sh" --verbose