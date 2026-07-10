#!/usr/bin/env bash
# Obtain the initial Let's Encrypt certificate so nginx can start with valid TLS.
#
# nginx refuses to start if the referenced certificate files are missing, so this
# script first drops a temporary self-signed certificate, starts nginx, then
# replaces it with a real certificate via the webroot ACME challenge. Ongoing
# renewal is handled automatically by the long-running certbot service.
#
# Run once, on the Ubuntu host, from the sassedo-be directory:
#   ./deploy/init-letsencrypt.sh
set -euo pipefail

cd "$(dirname "$0")/.."

if [ ! -f .env ]; then
  echo "ERROR: .env not found. Copy .env.example to .env and fill it in first." >&2
  exit 1
fi

# shellcheck disable=SC1091
set -a; . ./.env; set +a

: "${APP_DOMAIN:?APP_DOMAIN must be set in .env}"
: "${LETSENCRYPT_EMAIL:?LETSENCRYPT_EMAIL must be set in .env}"

STAGING="${LETSENCRYPT_STAGING:-0}"
COMPOSE="docker compose"
CERT_PATH="/etc/letsencrypt/live/${APP_DOMAIN}"

echo "### Requesting certificate for domain: ${APP_DOMAIN} (staging=${STAGING})"

echo "### Creating a temporary self-signed certificate so nginx can boot ..."
$COMPOSE run --rm --entrypoint "\
  sh -c 'mkdir -p ${CERT_PATH} && \
  openssl req -x509 -nodes -newkey rsa:2048 -days 1 \
    -keyout ${CERT_PATH}/privkey.pem \
    -out ${CERT_PATH}/fullchain.pem \
    -subj \"/CN=localhost\"'" certbot

echo "### Starting nginx ..."
$COMPOSE up -d nginx

echo "### Removing the temporary certificate ..."
$COMPOSE run --rm --entrypoint "\
  sh -c 'rm -rf /etc/letsencrypt/live/${APP_DOMAIN} \
    /etc/letsencrypt/archive/${APP_DOMAIN} \
    /etc/letsencrypt/renewal/${APP_DOMAIN}.conf'" certbot

staging_arg=""
if [ "$STAGING" != "0" ]; then
  staging_arg="--staging"
fi

echo "### Requesting the Let's Encrypt certificate ..."
$COMPOSE run --rm --entrypoint "\
  certbot certonly --webroot -w /var/www/certbot \
    ${staging_arg} \
    --email ${LETSENCRYPT_EMAIL} \
    -d ${APP_DOMAIN} \
    --rsa-key-size 4096 \
    --agree-tos \
    --no-eff-email \
    --force-renewal" certbot

echo "### Reloading nginx ..."
$COMPOSE exec nginx nginx -s reload

echo "### Done. Bring up the full stack with: docker compose up -d"
