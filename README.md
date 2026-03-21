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
