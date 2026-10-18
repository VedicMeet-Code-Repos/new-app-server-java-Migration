#!/bin/bash
set -euo pipefail
echo "Validating service..."
sleep 10
if systemctl is-active --quiet vedicmeet; then
    echo "Validation successful — vedicmeet service is running."
    exit 0
else
    echo "Validation FAILED — vedicmeet service is not running."
    systemctl status vedicmeet || true
    exit 1
fi
