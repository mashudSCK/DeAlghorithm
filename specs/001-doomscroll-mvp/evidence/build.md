# Android scaffold build evidence

Date: 2026-10-08 (Asia/Manila).

## Scaffold and pinned versions

The repository now contains one native Kotlin/Compose application module with
application ID `com.mashudsck.dealghorithm`, minimum SDK 29, and compile/target SDK 36.
The activity is a temporary shell; no feature behavior has been implemented.

| Component | Pinned version |
| --- | --- |
| Gradle wrapper | 8.13, binary distribution |
| Android Gradle plugin | 8.13.2 |
| Kotlin and Compose compiler plugin | 2.2.21 |
| Java language/toolchain | 17 |
| Compose BOM | 2024.09.00 |
| Activity Compose | 1.9.2 |
| Lifecycle | 2.8.6 |
| Core KTX | 1.13.1 |
| Preferences DataStore | 1.2.1 |
| JUnit | 4.13.2 |
| AndroidX test JUnit / runner / Espresso | 1.2.1 / 1.6.1 / 3.6.1 |

The installed Android Studio Empty Activity template specifies Compose BOM
2024.09.00; this was inspected read-only from `wizard-template.jar` using `javap`.
The compatible build-plugin baseline was adapted for the requested API 36 and
JDK 17 rather than retaining the cache's older AGP 8.5.2. Android's
[AGP 8.13 compatibility documentation](https://developer.android.com/build/releases/agp-8-13-0-release-notes)
supports Gradle 8.13, JDK 17 and API 36; its setup example pins Kotlin 2.2.21.
The [Compose compiler setup](https://developer.android.com/develop/ui/compose/setup-compose-dependencies-and-compiler)
requires the compiler plugin version to match Kotlin.

Installed tools used: Eclipse Adoptium JDK 17.0.18.8; Android SDK at
`C:\Android\Sdk`, which already contains platform 36 and build tools 35.0.0 and
36.0.0. `local.properties` points to that SDK and is ignored. No SDK installation
or global configuration change was performed.

## Commands and outcomes

The real wrapper JAR and scripts were generated with the cached Gradle 8.13
distribution in an ignored empty bootstrap project:

```powershell
gradle.bat -p .setup-cache/wrapper-bootstrap wrapper --gradle-version 8.13 --distribution-type bin --no-daemon
```

Result: exit 0, `BUILD SUCCESSFUL in 26s`, one wrapper task executed. The generated
`gradlew`, `gradlew.bat`, wrapper JAR and properties were copied into the project.

The Android gate was requested through the project wrapper:

```powershell
.\gradlew.bat assembleDebug testDebugUnitTest lintDebug --console=plain
```

The first attempt stalled during dependency resolution and was cancelled. A retry
with finite HTTP timeouts exited 1 after 9 minutes 17 seconds:

```powershell
.\gradlew.bat assembleDebug testDebugUnitTest lintDebug --console=plain '-Dorg.gradle.internal.http.connectionTimeout=10000' '-Dorg.gradle.internal.http.socketTimeout=10000'
```

Failure: required AGP and Kotlin artifacts could not be downloaded. Gradle reports
`No such host is known (dl.google.com)` and connection timeouts for Google Maven
and Maven Central. An independent PowerShell HEAD request to Maven Central
returned HTTP 200, but Google Maven, its official CDN and mirror probes failed;
that single HEAD response does not establish reliable Gradle downloads.

An offline check also exited 1 in 9 seconds:

```powershell
.\gradlew.bat assembleDebug testDebugUnitTest lintDebug --offline --console=plain --no-daemon
```

Decisive error: `Could not download gradle-8.13.2.jar
(com.android.tools.build:gradle:8.13.2): No cached version available for offline mode`.
Other required plugin artifacts are also missing from the cache.

A fresh single-use JVM retry forced IPv4 and set finite connection/read timeouts:

```powershell
.\gradlew.bat --no-daemon '-Djava.net.preferIPv4Stack=true' '-Dorg.gradle.jvmargs=-Xmx2048m -Dfile.encoding=UTF-8 -Djava.net.preferIPv4Stack=true -Dorg.gradle.internal.http.connectionTimeout=10000 -Dorg.gradle.internal.http.socketTimeout=10000' --console=plain assembleDebug testDebugUnitTest lintDebug
```

Result: exit 1, `BUILD FAILED in 10m 11s`, before Android compilation, tests or lint.
Required JAR downloads from both declared repositories still reported connection
timeouts; some subsequent Google requests reported DNS failures. The ignored log
is `build/gradle-gate-retry.log`. These are per-connection timeouts; dependency
retries mean they do not bound total build duration.

Separate escalated PowerShell requests now returned HTTP 200 for the official
AGP 8.13.2 POM on `dl.google.com`, `maven.google.com`, and the alternate official
Google path, plus the Kotlin 2.2.21 POM on Maven Central. PowerShell GET fetched the
AGP POM (11,773 bytes). `curl.exe --ipv4` fetched that POM and the exact AGP JAR
that Gradle failed to download (1,998,848 bytes), both HTTP 200; its IPv6 probe
failed. A bounded Java 17 `HttpURLConnection` probe with IPv4 forced still reported
`java.net.SocketTimeoutException: Connect timed out`. Thus a working OS-level
download route was demonstrated, but neither a fresh JVM nor IPv4 established
working Gradle artifact transport. No repository, proxy, firewall, DNS, or global
tool configuration was changed by these probes.

## Successful recovery on 2026-10-09

OS-level `curl.exe --ipv4` downloads worked while JVM repository transport failed.
Official Google Maven and Maven Central artifacts were staged into an ignored
temporary file repository under `.setup-cache/maven`. An ignored Gradle init script
prepended that repository for this local invocation; committed repositories remain
the official endpoints. No mirror, proxy, firewall or global configuration changed.
Available published SHA-1 checksums were checked for binary downloads.

```powershell
.\gradlew.bat --offline -I .setup-cache/local-official.init.gradle --no-daemon :app:assembleDebug :app:testDebugUnitTest :app:lintDebug --console=plain
```

Result: exit 0, `BUILD SUCCESSFUL in 1m 9s`; 50 tasks, five executed and 45
up-to-date. The debug APK exists at `app/build/outputs/apk/debug/app-debug.apk`.
Lint completed and wrote `app/build/reports/lint-results-debug.html`. The unit-test
task reported `NO-SOURCE`: this verifies scaffold task configuration, not feature
behavior. Feature tests and device validation remain pending. The ignored local
log is `build/local-gate.log`.

## Gate status and next action

Subsequent UI instrumentation found that Espresso 3.6.1 reflects the removed
`InputManager.getInstance` method on the Android 16 emulator. The test-only Espresso
pin is now 3.7.0. The official [AndroidX Test release notes](https://developer.android.com/jetpack/androidx/releases/test#espresso-3.7.0)
identify its replacement with `getSystemService`. Production dependencies are
unchanged. The manual feature gate must verify the updated test dependency.

T001-T003 are complete. Foundation work can proceed. Ordinary builds still require
working JVM transport or the ignored local staging workaround on this machine.
No physical-device support is claimed by the scaffold build.

The source manifest has no INTERNET, accessibility, overlay, exact-alarm or broad
app-inventory permission. This is a source check only; merged-manifest verification
still requires a successful Android build. Backup exclusions belong to T006.

T001-T003 task markers are checked. Reviewer checklist markers remain unchanged.
No commit was made.
