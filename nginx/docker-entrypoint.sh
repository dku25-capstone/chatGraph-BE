#!/bin/bash
set -e

CERT_PATH=/etc/nginx/ssl/selfsigned.crt
KEY_PATH=/etc/nginx/ssl/selfsigned.key

if [ ! -f "$CERT_PATH" ] || [ ! -f "$KEY_PATH" ]; then
  echo "[INFO] No SSL certs found, generating self-signed certs..."
  openssl req -x509 -nodes -days 365 -newkey rsa:2048 \
    -keyout $KEY_PATH \
    -out $CERT_PATH \
    -subj "/CN=$(hostname -I | awk '{print $1}')"
else
  echo "[INFO] SSL certs already exist, skipping generation."
fi

exec nginx -g 'daemon off;'