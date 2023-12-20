#!/bin/bash

while sudo lsof -i :80; do
  echo "Waiting for open port 80..."
  sleep 0.1
done
sudo nohup java -jar -Dspring.profiles.active=prod /home/ec2-user/rhododendra-0.0.1-SNAPSHOT.jar >> log.log 2>&1 &
