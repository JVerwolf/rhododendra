#!/bin/bash

DOMAIN="rhododendra.com"

# Install Java
sudo yum update
sudo yum install java-17-amazon-corretto

# Install certbot for managing SSL certs.  Might be included in base OS in future.
sudo dnf install -q -y augeas-libs
sudo python3 -m venv /opt/certbot/
sudo /opt/certbot/bin/pip install --upgrade pip
sudo /opt/certbot/bin/pip install certbot
sudo ln -s /opt/certbot/bin/certbot /usr/bin/certbot

# Acquire the cert. Don't do this too often while testing - it will incur weekly rate limiting. Use staging env instead.
sudo certbot certonly -a standalone -d "${DOMAIN}" -d "www.${DOMAIN}"

# Add crontab for updating cert.
echo "0 0,12 * * * root /opt/certbot/bin/python -c 'import random; import time; time.sleep(random.random() * 3600)' && sudo certbot renew -q --pre-hook '/home/ec2-user/stop.sh' --post-hook '/home/ec2-user/start.sh'" | sudo tee -a /etc/crontab > /dev/null

#sudo certbot renew -q --dry-run --pre-hook '/home/ec2-user/stop.sh' --post-hook '/home/ec2-user/start.sh'