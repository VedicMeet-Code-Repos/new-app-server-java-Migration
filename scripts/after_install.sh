#!/usr/bin/env bash
set -euo pipefail

env_file="/etc/vedicmeet/appserver.env"
jar="/opt/vedicmeet/new-app-server-java-0.0.1-SNAPSHOT.jar"

# 1. Ensure the environment file exists and is readable before anything else.
if [[ ! -f "$env_file" ]]; then
    echo "ERROR: environment file ${env_file} is missing. It should have been created by the BeforeInstall hook (scripts/before_install.sh) from SSM parameter /vedicmeet/appserver-env." >&2
    exit 1
fi

if [[ ! -r "$env_file" ]]; then
    echo "ERROR: environment file ${env_file} exists but is not readable." >&2
    exit 1
fi

# 2. Source the env file so KEY=VALUE pairs are available to this script.
set -a
# shellcheck disable=SC1090
source "$env_file"
set +a

# 3. Validate the application jar is present.
if [[ ! -f "$jar" ]]; then
    echo "ERROR: expected application jar at ${jar}." >&2
    exit 1
fi

chmod 0644 "$jar"
systemctl daemon-reload
echo "Application files installed and environment loaded from ${env_file}."
