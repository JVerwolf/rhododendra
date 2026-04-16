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

## PostgreSQL setup

**Version:** standardize on **PostgreSQL 16** (matches the embedded test binaries in `build.gradle.kts` and `postgresql16-*` on Amazon Linux 2023).

Override connection with `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, and `SPRING_DATASOURCE_PASSWORD` when the database is remote or credentials differ.

### macOS (Homebrew)

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
sudo ./scripts/setup-postgres-amazon-linux-2023.sh
```

To create the `rhododendra` role and database in the same step, set a strong password first:

```bash
export POSTGRES_APP_PASSWORD='your-strong-secret'
sudo -E ./scripts/setup-postgres-amazon-linux-2023.sh
```

The script is intended for Amazon Linux 2023 only. To run it on another OS (unsupported), set `FORCE=1` before `sudo -E`.

Then set `SPRING_DATASOURCE_PASSWORD` (or `start.sh` on the server) to match. For same-instance deployments, keep JDBC at `jdbc:postgresql://localhost:5432/rhododendra` and restrict PostgreSQL to local connections in `pg_hba.conf` unless you intentionally expose the port (use security groups and TLS for remote databases).

**Note:** [`setup.sh`](setup.sh) does **not** install PostgreSQL automatically (certbot/Java-focused); use the script above or your own automation. The script is **idempotent** (safe to re-run: skips existing cert, venv, and certbot renew cron entry).

## Running the server (local)

From the project root:

```bash
./gradlew bootRun
```

By default the app reads [`src/main/resources/application.properties`](src/main/resources/application.properties):

- **HTTP port:** `8090`
- **Domain** (templates/links): `domain` (e.g. `https://rhododendra.com`)

Open [http://localhost:8090](http://localhost:8090).

### Local sign-in (Google / Facebook OAuth)

Sign-in uses Spring Security OAuth2; the app does **not** store passwords. Credentials come from environment variables (see [`application.properties`](src/main/resources/application.properties) for the exact property names Spring maps them to).

**Google ([Google Cloud Console](https://console.cloud.google.com/) → APIs & Services → Credentials)**

1. Create an OAuth **Client ID** of type **Web application** (not Android, iOS, or Desktop).
2. Under **Authorized redirect URIs**, add the callback Spring Security uses:

   `http://localhost:8090/login/oauth2/code/google`

3. If you browse the app as `http://127.0.0.1:8090` instead of `localhost`, add a second redirect URI with `127.0.0.1`—Google treats hostnames literally.
4. Set real values locally (example):

   ```bash
   export GOOGLE_CLIENT_ID="your-id.apps.googleusercontent.com"
   export GOOGLE_CLIENT_SECRET="your-secret"
   ./gradlew bootRun
   ```

   Unset variables fall back to dummy placeholders and **will not** work with Google. If the consent screen is in **Testing** mode, add your Google account under **Test users**.

**Facebook ([Meta for Developers](https://developers.facebook.com/))**

- Add **Facebook Login** and set **Valid OAuth Redirect URIs** to:

  `http://localhost:8090/login/oauth2/code/facebook`

- Set `FACEBOOK_CLIENT_ID` and `FACEBOOK_CLIENT_SECRET` the same way as Google.

**After logout:** each “Continue with …” link sends extra OAuth parameters so Google shows an **account chooser** (`prompt=select_account`) and Facebook prompts for **login again** (`auth_type=reauthenticate`). That avoids the browser silently reusing the last Google/Facebook session without letting you pick a different account or provider. Implementation: [`ReauthenticationOAuth2AuthorizationRequestResolver`](src/main/java/com/rhododendra/config/ReauthenticationOAuth2AuthorizationRequestResolver.java).

To use another profile (e.g. production SSL settings in `application-prod.properties`):

```bash
./gradlew bootRun --args='--spring.profiles.active=prod'
```

To build a runnable JAR:

```bash
./gradlew bootJar
java -jar build/libs/rhododendra-0.0.1-SNAPSHOT.jar
```

## Deploying to EC2

`deploy.sh` deploys app binaries separately from runtime data paths; **PostgreSQL runs as its own service** on the server (or elsewhere). Lucene indexes live under the remote `data/` tree.

**SSH key:** pass the path to your SSH **private** key as a **positional argument** only (see below). There is no default key path and no environment-variable override in `deploy.sh`.

Default remote layout:

- app files: `/home/ec2-user/rhododendra/app`
- scripts: `/home/ec2-user/rhododendra/bin`
- data root: `/home/ec2-user/rhododendra/data`
- Lucene indexes: `/home/ec2-user/rhododendra/data/index`
- PostgreSQL: installed on the instance (or another host); set `SPRING_DATASOURCE_*` for `start.sh` (see below)

Basic deploy:

```bash
# Production — key is the only argument (environment defaults to prod)
./deploy.sh /path/to/ssh_private_key

# Production (explicit) or staging — two arguments
./deploy.sh prod /path/to/ssh_private_key
./deploy.sh staging /path/to/ssh_private_key
```

Database backups use PostgreSQL tools: [`scripts/pg_backup.sh`](scripts/pg_backup.sh) (`pg_dump` custom format) and restore with [`scripts/pg_restore.sh`](scripts/pg_restore.sh). Deploy does not copy a database file.

Notes:

- Properties files are packaged in the JAR; deploy does not copy `application*.properties` separately.
- The script prints `[deploy] …` steps as it runs. **Syncing the Lucene index** (`rsync`) is often the slow part; you will see per-file progress there.
- SSH is invoked with **BatchMode** (no password prompts) and a **connect timeout**, so a bad key or network fails instead of hanging. If your private key is **passphrase-protected**, load it into `ssh-agent` first (`ssh-add`); BatchMode cannot prompt for a passphrase.

**SSH “Connection timed out” / banner exchange errors:** that means your machine never got a working SSH session to the host (not an application bug in Rhododendra). Check in order: EC2 instance is **running**, you’re using the **current** public IP or Elastic IP (IPs change if the instance was replaced), the **security group** allows inbound **TCP 22** from **your current public IP** (or `0.0.0.0/0` only if you accept that risk), and whether you must use **VPN** or a **bastion**. Confirm with:

```bash
ssh -i /path/to/ssh_private_key ec2-user@<instance-host-or-ip> true
```

Override the host in deploy without editing the script: `PROD_HOST=x.x.x.x ./deploy.sh prod /path/to/ssh_private_key` (and `STAGING_HOST=...` for staging).

### `start.sh` and `stop.sh` (on the server)

`deploy.sh` copies these to the remote `bin/` directory (default: `/home/ec2-user/rhododendra/bin/`). They are meant to run **on the EC2 instance** (or via `ssh`); they are not required for local development.

**`start.sh`** — starts the app in the background:

- Ensures `app/` and `data/` exist under `REMOTE_BASE_DIR`.
- Changes working directory to `REMOTE_DATA_DIR` so Lucene resolves `./index` to `…/data/index` (same tree `deploy.sh` syncs to).
- Waits until nothing is listening on port **80** (same behavior as before SSL termination / binding on 80), up to **60 seconds**, then exits with an error so a stuck process cannot loop forever.
- Runs `java` with `-Dspring.profiles.active=$PROFILE` (default **`prod`**) and `-jar` pointing at the deployed JAR. **PostgreSQL** is configured with environment variables `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, and optionally `SPRING_DATASOURCE_PASSWORD` (omit the last to use the password baked into `application.properties` in the JAR, which defaults to `rhododendra` for local-style installs only—set a real secret in production).
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
| `SPRING_DATASOURCE_PASSWORD` | start | *(unset)* | If unset, Spring uses `application.properties` default; set in production. |
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
