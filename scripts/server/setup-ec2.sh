#!/usr/bin/env bash
#
# Idempotent EC2 host bootstrap (Java, certbot, TLS cert, renew cron). Safe to re-run.
#
# Full OS package update is skipped by default so repeat runs stay predictable. To run
# `yum update` first (previous behavior), set: RHODODENDRA_RUN_OS_UPDATE=1
#
set -euo pipefail

DOMAIN="rhododendra.com"
# Match deploy.sh / README: start.sh and stop.sh live under rhododendra/bin after deploy.
REMOTE_BIN_DIR="${REMOTE_BIN_DIR:-/home/ec2-user/rhododendra/bin}"

# Install Java
if [[ "${RHODODENDRA_RUN_OS_UPDATE:-0}" == "1" ]]; then
  echo "[setup-ec2] RHODODENDRA_RUN_OS_UPDATE=1 — running yum update..."
  sudo yum update -y
else
  echo "[setup-ec2] Skipping yum update (set RHODODENDRA_RUN_OS_UPDATE=1 to enable)."
fi
sudo yum install -y java-17-amazon-corretto

# PostgreSQL is not installed by this script. On Amazon Linux 2023 use (default path after deploy):
#   sudo /home/ec2-user/rhododendra/bin/setup-postgres-amazon-linux-2023.sh
# (optional POSTGRES_APP_PASSWORD for role/db creation). See README for macOS (Homebrew).

# Install certbot for managing SSL certs.  Might be included in base OS in future.
sudo dnf install -q -y augeas-libs
if [[ ! -x /opt/certbot/bin/pip ]]; then
  echo "[setup-ec2] Creating Python venv at /opt/certbot ..."
  sudo python3 -m venv /opt/certbot/
fi
sudo /opt/certbot/bin/pip install --upgrade pip
sudo /opt/certbot/bin/pip install certbot
sudo ln -sf /opt/certbot/bin/certbot /usr/bin/certbot

# Acquire the cert. Don't do this too often while testing - it will incur weekly rate limiting. Use staging env instead.
LE_LIVE="/etc/letsencrypt/live/${DOMAIN}"
if [[ -f "${LE_LIVE}/fullchain.pem" ]]; then
  echo "[setup-ec2] Certificate already present under ${LE_LIVE}; skipping certbot certonly."
else
  sudo certbot certonly -a standalone -d "${DOMAIN}" -d "www.${DOMAIN}"
fi
#sudo certbot certonly -a standalone -d "rhododendra.org" -d "www.rhododendra.org"
#sudo certbot certonly -a standalone -d "rhododendra.com" -d "www.rhododendra.com"

# Renew cron (single entry; marker prevents duplicate lines on re-run)
CRON_MARKER="rhododendra-certbot-renew"
CRON_LINE="0 0,12 * * * root /opt/certbot/bin/python -c 'import random; import time; time.sleep(random.random() * 3600)' && sudo certbot renew -q --pre-hook '${REMOTE_BIN_DIR}/stop.sh' --post-hook '${REMOTE_BIN_DIR}/start.sh'"
if [[ -f /etc/crontab ]] && grep -qF "${CRON_MARKER}" /etc/crontab; then
  echo "[setup-ec2] Certbot renew cron already present; skipping."
else
  echo "${CRON_LINE} # ${CRON_MARKER}" | sudo tee -a /etc/crontab > /dev/null
fi

#sudo certbot renew --dry-run --pre-hook '/home/ec2-user/rhododendra/bin/stop.sh' --post-hook '/home/ec2-user/rhododendra/bin/start.sh'