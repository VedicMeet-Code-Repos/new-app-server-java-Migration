#!/usr/bin/env bash
set -euo pipefail

jar="/opt/vedicmeet/new-app-server-java.jar"

if [[ ! -f "$jar" ]]; then
    echo "Deployment failed: expected application jar at $jar." >&2
    exit 1
fi

if [[ ! -r /etc/vedicmeet/appserver.env ]]; then
    echo "Deployment failed: /etc/vedicmeet/appserver.env is missing or unreadable." >&2
    exit 1
fi

chmod 0644 "$jar"
systemctl daemon-reload
echo "Application files installed."
