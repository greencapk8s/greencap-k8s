#!/bin/bash
# Script to configure hosts file

set -e

echo "=========================================="
echo "🔧 Configuring hosts file"
echo "=========================================="

sudo bash -c 'echo "127.0.0.1 domain.local" >> /etc/hosts'

echo ""
echo "=========================================="
echo "✅ Hosts file configured successfully!"
echo "=========================================="