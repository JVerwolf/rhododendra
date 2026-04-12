# Rhododendra

Spring Boot site for [rhododendra.com](https://rhododendra.com): rhododendron data is stored in **SQLite** (source of truth), with **Apache Lucene** used for search and indexing.

## Requirements

- **Java 17** (see `java` version in `build.gradle.kts`)
- Uses the **Gradle wrapper** (`./gradlew`); no separate Gradle install required

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

`deploy.sh` now deploys app binaries separately from runtime data paths, so SQLite and Lucene can live outside the app directory.

**SSH key:** pass the path to your SSH **private** key as a **positional argument** only (see below). There is no default key path and no environment-variable override in `deploy.sh`.

Default remote layout:

- app files: `/home/ec2-user/rhododendra/app`
- scripts: `/home/ec2-user/rhododendra/bin`
- data root: `/home/ec2-user/rhododendra/data`
- SQLite: `/home/ec2-user/rhododendra/data/rhododendra.sqlite`
- Lucene indexes: `/home/ec2-user/rhododendra/data/index`

Basic deploy:

```bash
# Production — key is the only argument (environment defaults to prod)
./deploy.sh /path/to/ssh_private_key

# Production (explicit) or staging — two arguments
./deploy.sh prod /path/to/ssh_private_key
./deploy.sh staging /path/to/ssh_private_key
```

Optional DB sync (run from an interactive terminal; you will be prompted to type `yes` before the remote database is overwritten):

```bash
SYNC_DB=true ./deploy.sh staging /path/to/ssh_private_key
```

Useful overrides:

```bash
REMOTE_BASE_DIR=/home/ec2-user/rhododendra \
LOCAL_DB_PATH=./data/rhododendra.sqlite \
SYNC_DB=true \
./deploy.sh prod /path/to/ssh_private_key
```

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
- Runs `java` with `-Dspring.profiles.active=$PROFILE` (default **`prod`**), `-jar` pointing at the deployed JAR, and `--db.path=$REMOTE_DB_PATH`.
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
| `REMOTE_DATA_DIR` | start | `$REMOTE_BASE_DIR/data` | Working directory at runtime; holds DB + `index/`. |
| `REMOTE_DB_PATH` | start | `$REMOTE_DATA_DIR/rhododendra.sqlite` | Passed to Spring as `--db.path`. |
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

Tests use their own config under `src/test/resources/application.properties` (test DB path, JSON fixtures, etc.).

## Data migration and Lucene indexing

Load JSON from a data directory into **SQLite**, then rebuild **Lucene** indexes under `./index/`:

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

**Load order into SQLite:** botanists → hybridizers → photo details → rhodos (combined list above).

**Gradle property overrides** (optional):

| Property      | Role |
|---------------|------|
| `dataJsonDir` | Root directory containing the migration JSON files listed in the table above |
| `dbPath`      | SQLite database file path |
| `domain`      | Site URL used by app settings (e.g. sitemap-style links) |

**Examples:**

```bash
# Custom JSON source and DB location
./gradlew migrateAndIndex -PdataJsonDir=/path/to/scraper/outputs/data -PdbPath=/tmp/rhododendra.sqlite

# Local domain while developing
./gradlew migrateAndIndex -Pdomain=http://localhost:8090
```

**Defaults** for `migrateAndIndex` are defined in [`build.gradle.kts`](build.gradle.kts) (including a default `dataJsonDir` path). Runtime defaults for `bootRun` are in `application.properties` (`db.path`, `data.jsonDir`).

More detail: [`HELP.md`](HELP.md).

## Configuration (overview)

| Key / topic | Purpose |
|-------------|---------|
| `db.path` | SQLite file (e.g. `./data/rhododendra.sqlite`) |
| `data.jsonDir` | Root directory for migration JSON inputs |
| `domain` | Public site URL for the running app |
| Lucene indexes | Written under `./index/` (see `IndexService`) |

Edit `src/main/resources/application.properties` or pass overrides on the command line, e.g.:

```bash
./gradlew bootRun --args='--db.path=./data/rhododendra.sqlite --data.jsonDir=/path/to/json'
```

(Spring relaxed binding applies to property names.)
