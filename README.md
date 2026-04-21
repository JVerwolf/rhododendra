# Rhododendra

Spring Boot site for [rhododendra.com](https://rhododendra.com): rhododendron data is stored in **PostgreSQL**, with **Apache Lucene** used for search and indexing.

## License

Rhododendra is [free software](https://www.gnu.org/philosophy/free-sw.en.html) released under the [GNU General Public License v3.0 or later](https://www.gnu.org/licenses/gpl-3.0.html) (**GPL-3.0-or-later**). You may use, modify, and redistribute it commercially; if you distribute modified versions, the GPL requires you to share the corresponding source under the same license. See the [`LICENSE`](LICENSE) file for the full legal text.

Contributions are accepted under the same terms.

## Requirements

- **Java 17** (see `java` version in `build.gradle.kts`)
- Uses the **Gradle wrapper** (`./gradlew`); no separate Gradle install required
- **PostgreSQL 16** — supported for local development (macOS) and production EC2 (**Amazon Linux 2023** packages). The app only needs a JDBC URL; install and run the server separately. Details: [PostgreSQL setup](#postgresql-setup).

- Integration tests use **embedded PostgreSQL 16** (Zonky) automatically — no local Postgres install required for `./gradlew test`.

## Shell scripts

Scripts are grouped by **where they run**. Paths are from the repository root unless noted.

| Script | Environment | Purpose |
|--------|-------------|---------|
| [`deploy.sh`](deploy.sh) | **Developer / operator machine** (with repo + build) | Uploads JAR, Lucene `index/`, and shell helpers to EC2 over SSH, then restarts the app. |
| [`scripts/dev/setup-postgres-macos.sh`](scripts/dev/setup-postgres-macos.sh) | **macOS dev** | Idempotent Homebrew PostgreSQL 16 and `rhododendra` role/database. |
| [`scripts/server/setup-postgres-amazon-linux-2023.sh`](scripts/server/setup-postgres-amazon-linux-2023.sh) | **EC2 / Amazon Linux 2023** | Idempotent PostgreSQL 16 install and optional app role/DB. Same file is copied to `bin/` on each deploy. |
| [`scripts/server/setup-ec2.sh`](scripts/server/setup-ec2.sh) | **EC2** | Java 17, certbot, TLS certificate, renew cron. Copied to remote `bin/setup-ec2.sh` on deploy. |
| [`start.sh`](start.sh) / [`stop.sh`](stop.sh) | **EC2** (copied to `bin/` by deploy) | Start/stop the Spring Boot JAR. |
| [`scripts/server/pg_backup.sh`](scripts/server/pg_backup.sh) / [`scripts/server/pg_restore.sh`](scripts/server/pg_restore.sh) | **Any host with PostgreSQL client tools** (dev or server) | Logical backup (`pg_dump -Fc`) and restore (`pg_restore --clean`). Copied to remote `bin/` on deploy. |
| [`scripts/one_time_sqlite_to_postgres.sh`](scripts/one_time_sqlite_to_postgres.sh) | *Documentation only* | Prints migration options; does not modify data. |

## PostgreSQL setup

**Version:** standardize on **PostgreSQL 16** (matches the embedded test binaries in `build.gradle.kts` and `postgresql16-*` on Amazon Linux 2023).

Override connection with `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, and `SPRING_DATASOURCE_PASSWORD` when the database is remote or credentials differ.

### macOS (Homebrew)

**Idempotent one-shot:** from the repo root, run `./scripts/dev/setup-postgres-macos.sh` (installs `postgresql@16` if needed, starts the service, ensures the `rhododendra` role and database, checks readiness). Safe to re-run.

Or follow the manual steps:

1. Install and start the server (user service, survives reboots when Brew services are enabled):

   ```bash
   brew install postgresql@16
   brew services start postgresql@16
   ```

2. Ensure client tools and `psql` resolve to this version (Apple Silicon example):

   ```bash
   export PATH="/opt/homebrew/opt/postgresql@16/bin:$PATH"
   ```

3. Create role and database (adjust password for your environment):

   ```bash
   createuser rhododendra 2>/dev/null || true
   createdb -O rhododendra rhododendra 2>/dev/null || true
   psql -c "ALTER USER rhododendra WITH PASSWORD 'rhododendra';" postgres
   ```

4. Check readiness: `pg_isready -h localhost -p 5432` (use `brew info postgresql@16` if your port differs from 5432).

### Amazon Linux 2023 (EC2 or AL2023 host)

Use the idempotent script (installs `postgresql16-server`, initializes if needed, enables `postgresql-16`):

```bash
sudo ./scripts/server/setup-postgres-amazon-linux-2023.sh
```

To create the `rhododendra` role and database in the same step, set a strong password first:

```bash
export POSTGRES_APP_PASSWORD='your-strong-secret'
sudo -E ./scripts/server/setup-postgres-amazon-linux-2023.sh
```

The script is intended for Amazon Linux 2023 only. To run it on another OS (unsupported), set `FORCE=1` before `sudo -E`.

After `systemctl` reports the service active, the script waits on `pg_isready` (defaults: `PGHOST=localhost`, `PGPORT=5432`; override if your listen address or port differs). With `POSTGRES_APP_PASSWORD` set, re-runs are idempotent: they refresh the role password and run `ALTER DATABASE … OWNER TO rhododendra` if the database already exists.

Then set `SPRING_DATASOURCE_PASSWORD` (or `start.sh` on the server) to match. For same-instance deployments, keep JDBC at `jdbc:postgresql://localhost:5432/rhododendra` and restrict PostgreSQL to local connections in `pg_hba.conf` unless you intentionally expose the port (use security groups and TLS for remote databases).

**Note:** [`scripts/server/setup-ec2.sh`](scripts/server/setup-ec2.sh) does **not** install PostgreSQL automatically (certbot/Java-focused); use the script above or your own automation. It is **idempotent** (safe to re-run: skips existing cert, venv, and certbot renew cron entry). By default it **skips** `yum update` so repeat runs stay predictable; set `RHODODENDRA_RUN_OS_UPDATE=1` when you intentionally want a full OS package refresh first.

## Running the server (local)

From the project root:

```bash
./gradlew bootRun
```

By default the app reads [`src/main/resources/application.properties`](src/main/resources/application.properties):

- **HTTP port:** `8090`
- **Domain** (templates/links): `domain` (e.g. `https://rhododendra.com`)

Open [http://localhost:8090](http://localhost:8090).

### Secrets and environment (local)

Database passwords, OAuth client secrets, and other sensitive values **must not** be committed. The repo includes [`.env.example`](.env.example) with the variable names Spring expects (same names as in [`application.properties`](src/main/resources/application.properties)). Copy it to **`.env`** or **`.env.local`** in the project root (both are [gitignored](.gitignore)) and fill in real values.

**Run with a local env file:**

```bash
chmod +x scripts/dev/run-with-env.sh   # once
./scripts/dev/run-with-env.sh
```

By default that script loads **`.env`**. To use another path: `RHODODENDRA_ENV_FILE=/path/to/file ./scripts/dev/run-with-env.sh`.

**Manual alternative:**

```bash
set -a && source .env && set +a && ./gradlew bootRun
```

**Production-like profile locally:** `application-prod.properties` and `application-staging.properties` require **`SPRING_DATASOURCE_PASSWORD`** (no default baked in). Put it in `.env` when testing `prod` / `staging` profiles.

**On EC2 (production):** keep secrets **on the server** (or in AWS SSM Parameter Store / Secrets Manager with an instance IAM role). Do **not** add secrets to `deploy.sh` or `rsync` / `scp` them from your laptop—deploy only builds artifacts from git. See [Deploying to EC2](#deploying-to-ec2) → *Secrets on the server*.

**Optional hardening:** run the JVM as a **dedicated Linux user** (not shared `ec2-user`), use **systemd** with `EnvironmentFile=` instead of ad-hoc `nohup`, and restrict who can SSH or `sudo` on the box. Example unit: [`scripts/server/rhododendra.service.example`](scripts/server/rhododendra.service.example). For **privileged ports** (for example 80 and 443) without running the JVM as root, use `AmbientCapabilities=CAP_NET_BIND_SERVICE` on that user or terminate TLS on a reverse proxy.

### Local sign-in (Google / Facebook OAuth)

Sign-in uses Spring Security OAuth2; the app does **not** store passwords. Credentials come from environment variables (see [`application.properties`](src/main/resources/application.properties) for the exact property names Spring maps them to).

**Google ([Google Cloud Console](https://console.cloud.google.com/) → APIs & Services → Credentials)**

1. Create an OAuth **Client ID** of type **Web application** (not Android, iOS, or Desktop).
2. Under **Authorized redirect URIs**, add the callback Spring Security uses:

   `http://localhost:8090/login/oauth2/code/google`

3. If you browse the app as `http://127.0.0.1:8090` instead of `localhost`, add a second redirect URI with `127.0.0.1`—Google treats hostnames literally.
4. Set real values in **`.env`** (see [Secrets and environment (local)](#secrets-and-environment-local)) or export in your shell, then run `./scripts/dev/run-with-env.sh` or `./gradlew bootRun`.

   Unset variables fall back to dummy placeholders and **will not** work with Google. If the consent screen is in **Testing** mode, add your Google account under **Test users**.

**Facebook ([Meta for Developers](https://developers.facebook.com/))**

- Add **Facebook Login** and set **Valid OAuth Redirect URIs** to:

  `http://localhost:8090/login/oauth2/code/facebook`

- Set `FACEBOOK_CLIENT_ID` and `FACEBOOK_CLIENT_SECRET` the same way as Google.

**After logout:** each “Continue with …” link sends extra OAuth parameters so Google shows an **account chooser** (`prompt=select_account`) and Facebook prompts for **login again** (`auth_type=reauthenticate`). That avoids the browser silently reusing the last Google/Facebook session without letting you pick a different account or provider. Implementation: [`ReauthenticationOAuth2AuthorizationRequestResolver`](src/main/java/com/rhododendra/config/ReauthenticationOAuth2AuthorizationRequestResolver.java).

To use another profile (e.g. production SSL settings in `application-prod.properties`), set **`SPRING_DATASOURCE_PASSWORD`** (and any OAuth vars you need) first—for example via `.env` and:

```bash
./scripts/dev/run-with-env.sh --args='--spring.profiles.active=prod'
```

To build a runnable JAR:

```bash
./gradlew bootJar
java -jar build/libs/rhododendra-0.0.1-SNAPSHOT.jar
```

## Deploying to EC2

`deploy.sh` deploys app binaries separately from runtime data paths; **PostgreSQL runs as its own service** on the server (or elsewhere). Lucene indexes live under the remote `data/` tree.

**First-time server setup (order):** install and configure PostgreSQL (for example [`scripts/server/setup-postgres-amazon-linux-2023.sh`](scripts/server/setup-postgres-amazon-linux-2023.sh)), optionally run host bootstrap [`scripts/server/setup-ec2.sh`](scripts/server/setup-ec2.sh) for Java/TLS, configure **secrets on the instance** (see *Secrets on the server* below), then use `deploy.sh` for application rollout. `deploy.sh` does not execute those bootstrap steps automatically (no surprise `sudo` on production).

**Secrets on the server:** `deploy.sh` only uploads the JAR, `start.sh` / `stop.sh`, and server helper scripts—it must **not** upload credential files from git (there should be none). On the EC2 host, create a root-owned env file **outside** the app tree, for example:

```bash
sudo mkdir -p /etc/rhododendra
sudo install -o root -g root -m 0600 /dev/null /etc/rhododendra/rhododendra.env
sudo nano /etc/rhododendra/rhododendra.env   # add KEY=value lines; see .env.example
```

[`start.sh`](start.sh) loads **`/etc/rhododendra/rhododendra.env`** by default when that path is readable (override with **`RHODODENDRA_ENV_FILE`**). Use the same variable names as [`.env.example`](.env.example), including **`SPRING_DATASOURCE_PASSWORD`** (required for `prod` and `staging` profiles).

**Stronger option:** store values in **AWS Systems Manager Parameter Store** (`SecureString`) or **Secrets Manager**, attach an **IAM instance profile** to the EC2 role with least-privilege read access, and use a small boot script or `ExecStartPre` to materialize `/etc/rhododendra/rhododendra.env` or export into the service environment—still no secrets in git.

For a **systemd-managed** process instead of `nohup`, see [`scripts/server/rhododendra.service.example`](scripts/server/rhododendra.service.example).

**SSH key:** pass the path to your SSH **private** key as a **positional argument** only (see below). There is no default key path and no environment-variable override in `deploy.sh`.

Default remote layout:

- app files: `/home/ec2-user/rhododendra/app`
- scripts: `/home/ec2-user/rhododendra/bin`
- data root: `/home/ec2-user/rhododendra/data`
- Lucene indexes: `/home/ec2-user/rhododendra/data/index`
- PostgreSQL: installed on the instance (or another host); set `SPRING_DATASOURCE_*` for `start.sh` (see below)

Each deploy copies into `bin/`: `start.sh`, `stop.sh`, `setup-ec2.sh`, `setup-postgres-amazon-linux-2023.sh`, `pg_backup.sh`, and `pg_restore.sh`, so the instance keeps current copies without a separate `git pull`. Override local sources with `SETUP_EC2_LOCAL`, `SETUP_POSTGRES_LOCAL`, `PG_BACKUP_LOCAL`, and `PG_RESTORE_LOCAL` if needed.

Basic deploy:

```bash
# Production — key is the only argument (environment defaults to prod)
./deploy.sh /path/to/ssh_private_key

# Production (explicit) or staging — two arguments
./deploy.sh prod /path/to/ssh_private_key
./deploy.sh staging /path/to/ssh_private_key
```

Database backups use PostgreSQL tools: [`scripts/server/pg_backup.sh`](scripts/server/pg_backup.sh) (`pg_dump` custom format) and restore with [`scripts/server/pg_restore.sh`](scripts/server/pg_restore.sh). Deploy does **not** copy database dump files or PostgreSQL data—only the helper scripts.

Notes:

- Properties files are packaged in the JAR; deploy does not copy `application*.properties` separately.
- The script prints `[deploy] …` steps as it runs. **Syncing the Lucene index** (`rsync --delete`) is often the slow part; you will see per-file progress there. The remote `data/index/` tree is made to **match** your local `index/` directory, including deletions of stale segment files. If your local `index/` is empty or wrong, the deploy will **wipe** the server’s search index to match—verify `index/` before deploying.
- SSH is invoked with **BatchMode** (no password prompts) and a **connect timeout**, so a bad key or network fails instead of hanging. If your private key is **passphrase-protected**, load it into `ssh-agent` first (`ssh-add`); BatchMode cannot prompt for a passphrase.

**SSH “Connection timed out” / banner exchange errors:** that means your machine never got a working SSH session to the host (not an application bug in Rhododendra). Check in order: EC2 instance is **running**, you’re using the **current** public IP or Elastic IP (IPs change if the instance was replaced), the **security group** allows inbound **TCP 22** from **your current public IP** (or `0.0.0.0/0` only if you accept that risk), and whether you must use **VPN** or a **bastion**. Confirm with:

```bash
ssh -i /path/to/ssh_private_key ec2-user@<instance-host-or-ip> true
```

Override the host in deploy without editing the script: `PROD_HOST=x.x.x.x ./deploy.sh prod /path/to/ssh_private_key` (and `STAGING_HOST=...` for staging).

### `start.sh` and `stop.sh` (on the server)

`deploy.sh` copies these (and the other `scripts/server/*.sh` helpers listed above) to the remote `bin/` directory (default: `/home/ec2-user/rhododendra/bin/`). They are meant to run **on the EC2 instance** (or via `ssh`); they are not required for local development.

**`start.sh`** — starts the app in the background:

- If a JVM is already running whose command line matches the deployed JAR file name, prints a message and **exits 0** (idempotent; avoids a second process on re-run).
- Ensures `app/` and `data/` exist under `REMOTE_BASE_DIR`.
- Changes working directory to `REMOTE_DATA_DIR` so Lucene resolves `./index` to `…/data/index` (same tree `deploy.sh` syncs to).
- Waits until nothing is listening on port **80** (same behavior as before SSL termination / binding on 80), up to **60 seconds**, then exits with an error so a stuck process cannot loop forever.
- If **`RHODODENDRA_ENV_FILE`** (default **`/etc/rhododendra/rhododendra.env`**) exists and is readable, **sources** it before starting Java so you can keep DB and OAuth variables off git (see [Secrets on the server](#deploying-to-ec2)).
- Runs `java` with `-Dspring.profiles.active=$PROFILE` (default **`prod`**) and `-jar` pointing at the deployed JAR. Forwards **`SPRING_DATASOURCE_*`** and, when set, **`GOOGLE_*`** / **`FACEBOOK_*`** into the process environment. With **`prod`** or **`staging`**, Spring requires **`SPRING_DATASOURCE_PASSWORD`** (no default in those profiles)—set it in the env file or shell.
- Appends stdout/stderr to `LOG_PATH` (default `…/app/log.log`) via `nohup`.

**`stop.sh`** — stops the Rhododendra JVM only:

- Finds processes with `pgrep` matching `java` and the deployed JAR **file name** (e.g. `rhododendra-0.0.1-SNAPSHOT.jar`), not the full path, so it still matches older invocations where the JAR lived directly under `~` before the `app/` layout.
- Sends `SIGTERM`, waits briefly, then `SIGKILL` if needed.

**Environment variables** (all optional; defaults match the layout above):

| Variable | Used by | Default | Purpose |
|----------|---------|---------|---------|
| `PROFILE` | start | `prod` | Spring profile (`prod` or `staging`). |
| `REMOTE_BASE_DIR` | both | `/home/ec2-user/rhododendra` | Root for `app/`, `data/`, `bin/`. |
| `REMOTE_APP_DIR` | both | `$REMOTE_BASE_DIR/app` | JAR and log location. |
| `REMOTE_DATA_DIR` | start | `$REMOTE_BASE_DIR/data` | Working directory at runtime; holds `index/` (Lucene). |
| `SPRING_DATASOURCE_URL` | start | `jdbc:postgresql://localhost:5432/rhododendra` | JDBC URL (use host/port for a dedicated DB server). |
| `SPRING_DATASOURCE_USERNAME` | start | `rhododendra` | PostgreSQL user. |
| `SPRING_DATASOURCE_PASSWORD` | start | *(unset)* | Required for **`prod`** / **`staging`** (set on the server). Optional for **`local`** (falls back to `application.properties`). |
| `RHODODENDRA_ENV_FILE` | start | `/etc/rhododendra/rhododendra.env` | If this path is **readable**, it is sourced (`KEY=value`) before `java` starts. |
| `JAR_PATH` | both | `$REMOTE_APP_DIR/rhododendra-0.0.1-SNAPSHOT.jar` | Which JAR to start / which process to stop. |
| `LOG_PATH` | start | `$REMOTE_APP_DIR/log.log` | Server log file for the `nohup` process. |

Examples over SSH (`YOUR_HOST` is the instance hostname or IP; use your own key path with `-i`):

```bash
# Stop then start production (defaults)
ssh -i /path/to/ssh_private_key ec2-user@YOUR_HOST \
  '/home/ec2-user/rhododendra/bin/stop.sh && /home/ec2-user/rhododendra/bin/start.sh'

# Staging profile
ssh -i /path/to/ssh_private_key ec2-user@YOUR_HOST \
  'PROFILE=staging /home/ec2-user/rhododendra/bin/start.sh'
```

## Running tests

```bash
./gradlew test
```

Test reports: `build/reports/tests/test/index.html`.

Tests use `src/test/resources/application.properties` (JSON fixtures, Lucene paths) and **embedded PostgreSQL** (Zonky) for JDBC.

## Data migration and Lucene indexing

Load JSON from a data directory into **PostgreSQL**, then rebuild **Lucene** indexes under `./index/`:

```bash
./gradlew migrateAndIndex
```

### Expected JSON files (`data.jsonDir`)

Migration reads **only** these paths, all relative to the directory set by `data.jsonDir` (see [`JSONLoaderService`](src/main/java/com/rhododendra/service/JSONLoaderService.java)):

| File | Content |
|------|---------|
| `species_botanists.json` | JSON **array** of botanist records (`Botanist`) |
| `hybridizers.json` | JSON **array** of hybridizer records (`Hybridizer`) |
| `photo_details.json` | JSON **array** of photo metadata records (`PhotoDetails`) |
| `species.json` | JSON **array** of rhododendron records (`Rhododendron`) — species |
| `hybrids.json` | JSON **array** of rhododendron records — hybrids |
| `azaleas.json` | JSON **array** of rhododendron records — azaleas |
| `vireyas.json` | JSON **array** of rhododendron records — vireyas |
| `azaleodendrons.json` | JSON **array** of rhododendron records — azaleodendrons |

Rhododendron rows are the **concatenation** of `species.json` + `hybrids.json` + `azaleas.json` + `vireyas.json` + `azaleodendrons.json` in that order. Empty arrays (`[]`) are valid for any file that has no rows.

**Load order into PostgreSQL:** botanists → hybridizers → photo details → rhodos (combined list above).

**Gradle property overrides** (optional):

| Property | Role |
|----------|------|
| `dataJsonDir` | Root directory containing the migration JSON files listed in the table above |
| `springDatasourceUrl` | JDBC URL (falls back to `SPRING_DATASOURCE_URL` env or `jdbc:postgresql://localhost:5432/rhododendra`) |
| `springDatasourceUsername` | DB user (env `SPRING_DATASOURCE_USERNAME` or `rhododendra`) |
| `springDatasourcePassword` | DB password (env `SPRING_DATASOURCE_PASSWORD` or `rhododendra`) |
| `domain` | Site URL used by app settings (e.g. sitemap-style links) |

**Examples:**

```bash
# Custom JSON source and JDBC target
./gradlew migrateAndIndex -PdataJsonDir=/path/to/scraper/outputs/data -PspringDatasourceUrl=jdbc:postgresql://localhost:5432/rhododendra

# Local domain while developing
./gradlew migrateAndIndex -Pdomain=http://localhost:8090
```

**Defaults** for `migrateAndIndex` are defined in [`build.gradle.kts`](build.gradle.kts) (including a default `dataJsonDir` path). Runtime defaults for `bootRun` are in `application.properties` (`spring.datasource.*`, `data.jsonDir`).

One-time migration from the old SQLite workflow is summarized in [`scripts/one_time_sqlite_to_postgres.sh`](scripts/one_time_sqlite_to_postgres.sh).

## Configuration (overview)

| Key / topic | Purpose |
|-------------|---------|
| [`.env.example`](.env.example) / local `.env` | Template and gitignored file for secrets and overrides (see [Secrets and environment (local)](#secrets-and-environment-local)). |
| `spring.datasource.url` / `SPRING_DATASOURCE_URL` | JDBC URL (include host/port for a remote PostgreSQL server) |
| `spring.datasource.username` / `SPRING_DATASOURCE_USERNAME` | Database user |
| `spring.datasource.password` / `SPRING_DATASOURCE_PASSWORD` | Database password |
| `data.jsonDir` | Root directory for migration JSON inputs |
| `domain` | Public site URL for the running app |
| Lucene indexes | Written under `./index/` (see `IndexService`) |

Edit `src/main/resources/application.properties` or pass overrides on the command line, e.g.:

```bash
./gradlew bootRun --args='--spring.datasource.url=jdbc:postgresql://db.example.com:5432/rhododendra --data.jsonDir=/path/to/json'
```

(Spring relaxed binding applies to property names.)
