#!/bin/bash
set -e

env_dir="/etc/vedicmeet"
env_file="${env_dir}/appserver.env"
ssm_param="/vedicmeet/appserver-env"
region="ap-south-1"

echo "[before_install] starting; whoami=$(whoami) PATH=$PATH"

# Resolve the AWS CLI explicitly. CodeDeploy runs hooks via the agent, whose
# PATH often omits /usr/local/bin where the AWS CLI v2 lives. A bare `aws`
# can fail with "command not found" — which is the usual reason this hook
# appears to succeed yet never writes the file.
AWS_BIN="$(command -v aws || true)"
if [ -z "$AWS_BIN" ]; then
    for candidate in /usr/local/bin/aws /usr/bin/aws /snap/bin/aws; do
        if [ -x "$candidate" ]; then
            AWS_BIN="$candidate"
            break
        fi
    done
fi
if [ -z "$AWS_BIN" ]; then
    echo "ERROR: aws CLI not found on PATH or common locations. Install AWS CLI on the instance." >&2
    exit 1
fi
echo "[before_install] using aws at: $AWS_BIN"

# 1. Create the config directory if it does not exist.
mkdir -p "$env_dir"
echo "[before_install] ensured directory $env_dir exists"

# 2. Fetch the SSM parameter with decryption. On any failure, exit 1 clearly.
echo "[before_install] fetching SSM parameter $ssm_param from $region"
if ! env_contents="$("$AWS_BIN" ssm get-parameter \
    --name "$ssm_param" \
    --with-decryption \
    --region "$region" \
    --query 'Parameter.Value' \
    --output text)"; then
    echo "ERROR: failed to fetch SSM parameter ${ssm_param} in ${region}. Check the instance IAM role has ssm:GetParameter (and kms:Decrypt for SecureString)." >&2
    exit 1
fi

if [ -z "$env_contents" ] || [ "$env_contents" = "None" ]; then
    echo "ERROR: SSM parameter ${ssm_param} returned an empty value." >&2
    exit 1
fi

# 3. Write the value to the env file.
printf '%s\n' "$env_contents" > "$env_file"
echo "[before_install] wrote $env_file"

# 4. Permissions 600, owned by root.
chown root:root "$env_file"
chmod 0600 "$env_file"

echo "[before_install] done. $(ls -l "$env_file")"
