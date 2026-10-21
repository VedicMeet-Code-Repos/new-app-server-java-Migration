#!/bin/bash
set -e

env_file="/etc/vedicmeet/appserver.env"
jar="/opt/vedicmeet/new-app-server-java-0.0.1-SNAPSHOT.jar"

echo "[after_install] starting; checking $env_file"

# 1. Ensure the environment file exists and is readable.
if [ ! -f "$env_file" ]; then
    echo "ERROR: ${env_file} is missing. The BeforeInstall hook (scripts/before_install.sh) should have created it from SSM parameter /vedicmeet/appserver-env." >&2
    exit 1
fi

if [ ! -r "$env_file" ]; then
    echo "ERROR: ${env_file} exists but is not readable by $(whoami)." >&2
    exit 1
fi

# 2. Source the file to load environment variables (KEY=VALUE per line).
set -a
# shellcheck disable=SC1090
. "$env_file"
set +a
echo "[after_install] loaded environment from $env_file"

# 3. Continue with the rest of the deployment steps.
if [ ! -f "$jar" ]; then
    echo "ERROR: expected application jar at ${jar}." >&2
    exit 1
fi

chmod 0644 "$jar"
systemctl daemon-reload
echo "[after_install] done. Application files installed and environment loaded."
