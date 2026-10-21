#!/bin/bash
set -e

env_file="/etc/vedicmeet/appserver.env"
service_file="/etc/systemd/system/vedicmeet.service"

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

# 3. Ensure the systemd unit exists (written by scripts/create_service.sh,
#    which runs earlier in the AfterInstall hook).
if [ ! -f "$service_file" ]; then
    echo "ERROR: ${service_file} is missing. scripts/create_service.sh should run before this script in the AfterInstall hook." >&2
    exit 1
fi

# 4. Register the service with systemd and enable it on boot.
systemctl daemon-reload
systemctl enable vedicmeet.service
echo "[after_install] vedicmeet.service registered and enabled"

echo "[after_install] done. Application files installed and service registered."
