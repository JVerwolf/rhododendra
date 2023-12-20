#!/bin/bash

sudo lsof -i tcp:80 | awk 'NR!=1 {print $2}' | xargs sudo kill -9
while sudo lsof -i :80; do
  echo "Waiting for open port 80..."
  sleep 0.1
done