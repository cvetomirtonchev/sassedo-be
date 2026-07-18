# sassedo-be

Spring Boot 4 (Java 17) REST API for Sassedo, backed by MySQL 8.4 and deployed with
Docker Compose behind nginx (TLS via Let's Encrypt).

- App: Spring Boot, port `8080` (management/actuator on `8081`)
- Database: MySQL 8.4
- Reverse proxy / TLS: nginx (`80`/`443`), certificates via certbot
- Full deployment guide: [docs/ubuntu-docker-deployment.md](docs/ubuntu-docker-deployment.md)

## Prerequisites

- Docker Engine + Docker Compose plugin (production/server)
- For local dev without Docker: JDK 17 and Maven, plus a local MySQL

## Environment

All secrets/config come from a git-ignored `.env` file. Copy the template and fill it in:

```bash
cp .env.example .env
```

Generate strong secrets:

```bash
openssl rand -base64 48   # JWT_SECRET
openssl rand -hex 16      # MySQL passwords
```

> `.env.example` contains placeholder values only. The running server uses its own `.env`;
> never rely on the example values to connect.

## Run locally (native, no Docker)

```bash
export SPRING_PROFILES_ACTIVE=dev
mvn spring-boot:run -Dspring-boot.run.jvmArguments="-Dlog-path=./logs"
```

- API: `http://localhost:8080`
- Health/metrics: `http://localhost:8081/actuator/health`

Build a jar:

```bash
mvn clean package            # add -DskipTests to skip the DB-dependent context test
```

## Deploy with Docker (server)

Standard production stack (domain + TLS). See the full guide for first-time setup and DNS.

```bash
# First-time TLS bootstrap (needs APP_DOMAIN + a real DNS record / hostname):
./deploy/init-letsencrypt.sh

# Start / update the full stack:
docker compose up -d --build
```

### Temporary HTTP-only access by IP (no domain yet)

```bash
docker compose -f compose.yaml -f compose.ip.yaml up -d --build
```

## Redeploy after backend code changes

From your machine, push the changes:

```bash
git add -A && git commit -m "your message" && git push origin main
```

On the server, pull and rebuild just the app container:

```bash
cd ~/sassedo-be
git pull origin main
sudo docker compose up -d --build app
```

Use `sudo docker compose up -d --build` (without `app`) if you also changed compose/nginx config.

## Operations

```bash
# Status and logs
sudo docker compose ps
sudo docker compose logs -f app

# Health (inside the container; actuator is internal-only)
sudo docker compose exec app curl -fsS http://127.0.0.1:8081/actuator/health

# Restart / stop (keeps the database volume)
sudo docker compose restart
sudo docker compose down

# Database backup / restore
sudo docker compose exec mysql sh -c 'exec mysqldump -uroot -p"$MYSQL_ROOT_PASSWORD" "$MYSQL_DATABASE"' > backup.sql
sudo docker compose exec -T mysql sh -c 'exec mysql -uroot -p"$MYSQL_ROOT_PASSWORD" "$MYSQL_DATABASE"' < backup.sql

# MySQL shell inside the container
sudo docker compose exec mysql mysql -u sassedo -p sassedodb
```

## Ports

| Service | Container port | Published |
|---------|----------------|-----------|
| app (HTTP API) | 8080 | no (proxied by nginx) |
| app (actuator) | 8081 | no (internal) |
| MySQL | 3306 | only if enabled via a local override |
| nginx | 80, 443 | yes |
