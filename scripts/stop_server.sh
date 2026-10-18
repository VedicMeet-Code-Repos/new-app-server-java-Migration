#!/bin/bash
set -euo pipefail
echo "Stopping vedicmeet application..."
systemctl stop vedicmeet || true
echo "Application stopped."
