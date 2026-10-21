#!/bin/bash
set -e

service_file="/etc/systemd/system/vedicmeet.service"

echo "[create_service] writing systemd unit to $service_file"

cat > "$service_file" <<'EOF'
[Unit]
Description=Vedicmeet Application Server
After=network.target

[Service]
EnvironmentFile=/etc/vedicmeet/appserver.env
ExecStart=/usr/bin/java -jar /opt/vedicmeet/vedicmeet.jar
SuccessExitStatus=143
Restart=on-failure
RestartSec=10

[Install]
WantedBy=multi-user.target
EOF

chmod 0644 "$service_file"
echo "[create_service] done. $(ls -l "$service_file")"
