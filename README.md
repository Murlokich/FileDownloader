# FileDownloader

A file downloader written in Kotlin with optional parallel chunk downloads.

## Tooling

This project uses **Gradle** for build, run, and test tasks.

## Building

```bash
./gradlew clean build
```

## Running

Run with a URL (default mode):

```bash
./gradlew run --args="https://example.com/file.zip"
```

Run with explicit chunk count for parallel mode:

```bash
./gradlew run --args="--chunks 4 https://example.com/file.zip"
```

## CLI usage

```bash
./gradlew run --args="[--chunks N] <URL>"
```

- `--chunks` / `-c` sets the number of chunks.
- Default is `1` chunk.

## Download flow

1. The app sends a `HEAD` request to inspect server metadata.
2. If range downloads are supported and chunks > 1, it downloads byte ranges in parallel, otherwise non-parallel download happens
3. By default (`chunks = 1`), it downloads the file in a single request using regular `GET`.
4. Downloaded bytes are written to a local file in the current working directory.

## Testing

Repo has multiple unit tests and one end-to-end test stored in: `/src/test/kotlin`

```bash
./gradlew test
```

