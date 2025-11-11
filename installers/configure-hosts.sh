#!/bin/bash
# Script to configure hosts file

set -e

echo "=========================================="
echo "Configuring hosts file"
echo "=========================================="

# Backup hosts file if backup doesn't exist yet
if [ -f /etc/hosts ] && [ ! -f /etc/hosts.gcbck ]; then
    echo "💾 Creating backup of /etc/hosts..."
    sudo cp /etc/hosts /etc/hosts.gcbck
    echo "Backup created: /etc/hosts.gcbck"
fi

sudo bash -c 'echo "# Added by Greencap installer:" >> /etc/hosts'
sudo bash -c 'echo "[begin:greencap]" >> /etc/hosts'

echo ""
echo "=========================================="
echo "Hosts file configured successfully."
echo "=========================================="