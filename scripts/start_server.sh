#!/bin/bash
set -e

echo "[start_server] starting vedicmeet.service"
systemctl start vedicmeet.service

# Verify the service actually came up.
if ! systemctl is-active --quiet vedicmeet.service; then
    echo "ERROR: vedicmeet.service failed to start. Recent logs:" >&2
    systemctl status vedicmeet.service --no-pager >&2 || true
    journalctl -u vedicmeet.service --no-pager -n 50 >&2 || true
    exit 1
fi

echo "[start_server] vedicmeet.service is active."
