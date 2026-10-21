#!/usr/bin/env bash
set -euo pipefail

env_dir="/etc/vedicmeet"
env_file="${env_dir}/appserver.env"
ssm_param="/vedicmeet/appserver-env"
region="ap-south-1"

# 1. Create the config directory if it doesn't already exist.
if ! mkdir -p "$env_dir"; then
    echo "ERROR: failed to create directory ${env_dir}." >&2
    exit 1
fi

# 2. Fetch the env contents from SSM Parameter Store.
#    --with-decryption decrypts SecureString parameters (no-op for String).
if ! env_contents="$(aws ssm get-parameter \
    --name "$ssm_param" \
    --with-decryption \
    --region "$region" \
    --query 'Parameter.Value' \
    --output text)"; then
    echo "ERROR: failed to fetch SSM parameter ${ssm_param} in ${region}." >&2
    exit 1
fi

if [[ -z "$env_contents" || "$env_contents" == "None" ]]; then
    echo "ERROR: SSM parameter ${ssm_param} is empty." >&2
    exit 1
fi

# 3. Write atomically: stage to a temp file, then move into place.
tmp_file="$(mktemp)"
if ! printf '%s' "$env_contents" > "$tmp_file"; then
    echo "ERROR: failed to write temporary env file." >&2
    rm -f "$tmp_file"
    exit 1
fi

if ! mv "$tmp_file" "$env_file"; then
    echo "ERROR: failed to move env file into ${env_file}." >&2
    rm -f "$tmp_file"
    exit 1
fi

# 4. Root-owned, world-readable (644).
if ! chown root:root "$env_file"; then
    echo "ERROR: failed to set ownership on ${env_file}." >&2
    exit 1
fi

if ! chmod 0644 "$env_file"; then
    echo "ERROR: failed to set permissions on ${env_file}." >&2
    exit 1
fi

echo "Wrote environment file to ${env_file} from SSM parameter ${ssm_param} (${region})."
