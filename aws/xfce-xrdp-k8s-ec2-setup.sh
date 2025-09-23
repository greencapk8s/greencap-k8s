#!/bin/bash

# First step:
sudo apt update && sudo apt upgrade -y
sudo reboot

# Second step:
sudo apt update && sudo apt upgrade -y
sudo apt install -y xfce4 xfce4-goodies xrdp firefox
echo xfce4-session > ~/.xsession
sudo usermod -aG ssl-cert $USER
sudo systemctl restart xrdp
sudo reboot

# Third step after reboot:
# Create password for user ubuntu.
# Test RDP connection via remote desktop(Remmina).

# Fourth step:
# Setup python and project virtual environment.
sudo DEBIAN_FRONTEND=noninteractive apt install -y python3-pip python3-venv
sudo apt install -y apt-transport-https
git clone https://github.com/joseafilho/k8s-kind-nginx-playground.git
mv k8s-kind-nginx-playground playground
cd playground
git checkout developer
cd ..
python3 -m venv ./playground/.venv
sudo chown -R ubuntu:ubuntu ./playground/.venv/
source ./playground/.venv/bin/activate
pip3 install -r ./playground/installers/requirements.txt

# Fifth step:
sudo python3 ./playground/installers/installer.py --script "./playground/installers/configure-docker-daemon.sh" --verbose
sudo python3 ./playground/installers/installer.py --script "./playground/installers/configure-hosts.sh" --verbose
sudo python3 ./playground/installers/installer.py --script "./playground/installers/kind-ec2-install.sh" --verbose
sudo python3 ./playground/installers/installer.py --script "./playground/installers/kubectl-ec2-install.sh" --verbose
sudo python3 ./playground/installers/installer.py --script "./playground/installers/helm-install.sh" --verbose
sudo python3 ./playground/installers/installer.py --script "./playground/installers/cilium-install.sh" --verbose
sudo python3 ./playground/installers/installer.py --script "./playground/installers/ingress-controller-install.sh" --verbose
sudo python3 ./playground/installers/installer.py --script "./playground/installers/apache-hello-install.sh" --verbose
sudo python3 ./playground/installers/installer.py --script "./playground/installers/kube-dash-install.sh" --verbose
sudo python3 ./playground/installers/installer.py --script "./playground/installers/harbor-install.sh" --verbose
sudo python3 ./playground/installers/installer.py --script "./playground/installers/postgres-install.sh" --verbose
sudo python3 ./playground/installers/installer.py --script "./playground/installers/ecom-python-install.sh" --verbose