# Isolated build (does NOT touch the shared `~/.m2`)

This project is built with a **dedicated Maven install + a dedicated local repository**, kept fully
separate from `C:\Users\<you>\.m2` (which belongs to another project and must not be disturbed).

## Layout (outside OneDrive, outside `~/.m2`)
```
C:\Users\tiwaris5\vedicmeet-build\
  apache-maven-3.9.16\      # standalone Maven (downloaded from dlcdn.apache.org)
  repo\                     # this project's ISOLATED local repository
```

## Build + test (PowerShell)
```powershell
$env:JAVA_HOME = "C:\Program Files\Java\jdk-17"          # project targets Java 17
$mvn  = "C:\Users\tiwaris5\vedicmeet-build\apache-maven-3.9.16\bin\mvn.cmd"
$repo = "C:\Users\tiwaris5\vedicmeet-build\repo"
Set-Location "<...>\Vedicmeet\new-app-server-java"

& $mvn "-Dmaven.repo.local=$repo" -B clean test
```
`-Dmaven.repo.local=$repo` forces Maven to use the isolated repo, so the shared `~/.m2` is never read
or written.

## Verified results (2026-08-31)
- `mvn clean compile` → **BUILD SUCCESS** — 95 source files (all of `payment/`, `call/`, `cron/`,
  `realtime/` including `SocketIoConfig` against `netty-socketio:2.0.9`, and `integrations/`), Java 17.
- `mvn test` → **Tests run: 105, Failures: 0, Errors: 0, Skipped: 0 — BUILD SUCCESS.**
  (Shadow-only migration tests + crypto/JWT parity tests + a full Spring `AppServerContextLoadTest`
  that boots the whole bean graph so a missing/ambiguous bean fails the build.)

Notes:
- Most tests are plain JUnit 5 unit tests with in-memory fakes (no Mongo/Redis). One test,
  `AppServerContextLoadTest`, starts the **full Spring context** (datastores stay lazy, so no live
  Mongo/Redis is needed) to guarantee the application actually boots and every bean wires.
  No socket port is bound during the build, so the suite is hermetic and safe.
- `SocketIoConfig` is compiled but **gated off** (`vedicmeet.socket.enabled=false`), so nothing binds a
  socket port during the build.
- The only remaining verification step is the **contract-harness diff against the TEST server**, which
  needs the running TEST env + tokens (not part of the unit build).
