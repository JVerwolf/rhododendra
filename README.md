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
