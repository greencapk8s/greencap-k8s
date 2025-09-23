#!/bin/bash
# Script to configure hosts file

set -e

echo "=========================================="
echo "🔧 Configuring hosts file"
echo "=========================================="

if grep -qE '^[[:space:]]*127\.0\.0\.1[[:space:]]+domain\.local([[:space:]]|$)' /etc/hosts; then
    echo "Entry '127.0.0.1 domain.local' already exists /etc/hosts, skipping..."
else
    sudo bash -c 'echo "127.0.0.1 domain.local" >> /etc/hosts'
fi

echo ""
echo "=========================================="
echo "✅ Hosts file configured successfully!"
echo "=========================================="