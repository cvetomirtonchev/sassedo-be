# Ubuntu 24.04 Docker Compose Deployment

This guide deploys `sassedo-be` (Spring Boot 4 / Java 17) on Ubuntu 24.04 using Docker
Compose. The stack is:

- `app` - the Spring Boot service, built from source with a multi-stage image, run with the `prod` profile.
- `mysql` - MySQL 8.4 with a persistent volume (isolated on an internal network).
- `nginx` - the only public entry point (ports 80/443), terminates TLS and reverse-proxies to `app`.
- `certbot` - obtains and renews the Let's Encrypt certificate.

Only nginx publishes host ports. `app` (8080/8081) and `mysql` (3306) are reachable only on the internal Docker networks.

## 1. Prerequisites

### Install Docker Engine + Compose plugin

```bash
sudo apt-get update
sudo apt-get install -y ca-certificates curl
sudo install -m 0755 -d /etc/apt/keyrings
sudo curl -fsSL https://download.docker.com/linux/ubuntu/gpg -o /etc/apt/keyrings/docker.asc
sudo chmod a+r /etc/apt/keyrings/docker.asc
echo \
  "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.asc] https://download.docker.com/linux/ubuntu \
  $(. /etc/os-release && echo "$VERSION_CODENAME") stable" | \
  sudo tee /etc/apt/sources.list.d/docker.list > /dev/null
sudo apt-get update
sudo apt-get install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin
```

Optionally allow your user to run docker without sudo:

```bash
sudo usermod -aG docker "$USER"   # log out / back in for it to take effect
```

### DNS

Point an `A` record (and `AAAA` if you have IPv6) for your `APP_DOMAIN` at this server's public IP.
The certificate cannot be issued until DNS resolves publicly.

### Firewall

Open HTTP and HTTPS (needed for the ACME challenge and normal traffic):

```bash
sudo ufw allow 80/tcp
sudo ufw allow 443/tcp
sudo ufw allow OpenSSH   # keep SSH access
sudo ufw enable
```

## 2. Configure the environment

```bash
cd sassedo-be
cp .env.example .env
# Edit .env: set APP_DOMAIN, LETSENCRYPT_EMAIL, all DB passwords, JWT_SECRET, and SMTP settings.
```

Generate strong secrets, for example:

```bash
openssl rand -base64 48   # JWT_SECRET
openssl rand -base64 24   # database passwords
```

> Security note: The database and SMTP passwords that previously lived in
> `application*.properties` are in git history. Externalizing them here does not remove
> them from history - rotate those credentials to new values.

## 3. Obtain the first TLS certificate

While testing, set `LETSENCRYPT_STAGING=1` in `.env` to avoid Let's Encrypt rate limits, then
switch back to `0` and re-run once the flow works.

```bash
./deploy/init-letsencrypt.sh
```

This creates a temporary self-signed cert, starts nginx, requests the real certificate over the
HTTP-01 webroot challenge, and reloads nginx. The `certbot` service then auto-renews it.

### Alternative: temporary access by IP (no domain yet)

Use this only while you do not have a domain/certificate. It serves the API over plain HTTP
(no TLS) on port 80 through nginx, keeping `app` and `mysql` on the internal network. Do not
run `deploy/init-letsencrypt.sh` in this mode.

1. Make sure port 80 is reachable:

   ```bash
   sudo ufw allow 80/tcp
   ```

   Also open inbound TCP 80 in the cloud firewall / AWS security group for this instance.

2. `APP_DOMAIN` in `.env` can keep any placeholder value; it is not used by the IP template.

3. Start the stack with the IP overlay (note the two `-f` flags):

   ```bash
   docker compose -f compose.yaml -f compose.ip.yaml up -d --build
   ```

4. Verify over HTTP using the public IP (example `3.121.162.2`):

   ```bash
   curl -I http://3.121.162.2
   curl http://3.121.162.2/api/faq/all
   ```

Because traffic is unencrypted, treat this as temporary. Switch to the domain + TLS flow below
as soon as DNS is ready.

#### Switching from IP mode to the domain + TLS setup

```bash
# 1. Point the domain's A record at the server and set APP_DOMAIN/LETSENCRYPT_EMAIL in .env
# 2. Stop the IP-mode nginx so port 80 is free for the ACME challenge
docker compose -f compose.yaml -f compose.ip.yaml down

# 3. Issue the certificate and bring up the standard (domain) stack
./deploy/init-letsencrypt.sh
docker compose up -d --build
```

From then on, always use the plain `docker compose ...` commands (without `-f compose.ip.yaml`).

## 4. Start the stack

```bash
docker compose up -d --build
```

Compose builds the app image (Maven build; tests are skipped in the image build because they
require a live database - run them in CI), starts MySQL, waits for it to become healthy, starts
the app, waits for its health check, then starts nginx.

## 5. Verify

```bash
docker compose ps                       # all services healthy/running
docker compose logs -f app              # application logs
docker compose exec app curl -fsS http://127.0.0.1:8081/actuator/health   # expect {"status":"UP"}
curl -I https://your-domain             # served through nginx with valid TLS
```

Rolling application log files are also persisted in the `app-logs` volume (`/app/logs/app.log`).

## 6. Operations

### Update to a new version

```bash
git pull
docker compose up -d --build
docker image prune -f
```

### Certificate renewal

Automatic: the `certbot` service attempts renewal twice a day and nginx reloads every 6 hours to
pick up renewed certs. Force a renewal manually if needed:

```bash
docker compose run --rm certbot renew --force-renewal
docker compose exec nginx nginx -s reload
```

### Database backup / restore

```bash
# Backup
docker compose exec mysql sh -c 'exec mysqldump -uroot -p"$MYSQL_ROOT_PASSWORD" "$MYSQL_DATABASE"' > backup.sql

# Restore
docker compose exec -T mysql sh -c 'exec mysql -uroot -p"$MYSQL_ROOT_PASSWORD" "$MYSQL_DATABASE"' < backup.sql
```

The database lives in the `mysql-data` named volume and survives container recreation.

### Rollback

```bash
git checkout <previous-tag-or-commit>
docker compose up -d --build
```

If a schema migration was applied (Hibernate `ddl-auto=update`), restore the database from the
most recent backup taken before the upgrade.

## Notes and limitations

- Schema is managed by Hibernate `ddl-auto=update`. For stricter production schema control,
  adopt Flyway/Liquibase (out of scope for this deployment).
- Realtime messaging uses in-memory SSE, so the `app` service must run as a single instance
  (do not scale it to multiple replicas without a shared gateway).
- Images are stored as MySQL BLOBs, so no object storage or extra volume mounts are required;
  size the disk/`mysql-data` volume accordingly.
- Actuator/Prometheus is on internal port 8081 and is not published to the host.
