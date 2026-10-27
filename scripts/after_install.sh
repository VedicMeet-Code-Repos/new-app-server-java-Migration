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

# 3. Verify the JAR was deployed.
if [ ! -f "$jar" ]; then
    echo "ERROR: expected application jar at ${jar}." >&2
    exit 1
fi

chmod 0644 "$jar"

# 4. Write the systemd service file on every deployment.
cat > /etc/systemd/system/vedicmeet.service << 'SVCEOF'
[Unit]
Description=Vedicmeet Java Application
After=network.target

[Service]
EnvironmentFile=/etc/vedicmeet/appserver.env
User=root
WorkingDirectory=/opt/vedicmeet
ExecStart=/usr/bin/java -jar /opt/vedicmeet/new-app-server-java-0.0.1-SNAPSHOT.jar
SuccessExitStatus=143
Restart=on-failure
RestartSec=5
StandardOutput=journal
StandardError=journal

[Install]
WantedBy=multi-user.target
SVCEOF

systemctl daemon-reload
systemctl enable vedicmeet
echo "[after_install] vedicmeet.service unit file written and enabled."

echo "[after_install] done. Application files installed and environment loaded."
