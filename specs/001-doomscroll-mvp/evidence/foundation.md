# Shared-state and privacy foundation

Date: 2026-10-09 (Asia/Manila).

T004-T006 are complete. Durable models enforce schema 1, disabled monitoring by
default, mode/package constraints, nonnegative counters, unique activity IDs,
ten activities and seven daily summaries. Purpose text and observation history
have no durable fields. Manual intervals partition local midnight without
double-counting and never increment measured app time.

One transactional Preferences DataStore resides beneath `noBackupFilesDir`.
Every read/write prunes dates outside today and the preceding six dates. Invalid
JSON and unsupported schemas raise errors without replacing the stored bytes.
The caller must expose the error and keep monitoring stopped; feature wiring
is not part of this foundation checkpoint.

Build results:

- Assembly, unit tests, lint and instrumented APK assembly passed: exit 0,
  `BUILD SUCCESSFUL in 1m 57s`, 80 tasks.
- The final midnight-partition change passed JVM tests and lint: exit 0,
  `BUILD SUCCESSFUL in 2m 4s`, 34 tasks. Three AppState JVM tests passed.
- Two StateStore instrumented tests passed on the API 36 / Android 16 emulator:
  `OK (2 tests)`. They verify retention, separate measured/manual totals, activity
  limits, transactional rejection and byte preservation for invalid/unknown schemas.

Commands used the ignored official-artifact staging init script documented in
[build.md](build.md). The corrected instrumented APK was installed using
`adb -s emulator-5554 install --no-streaming -r -t`, then tested with:

```powershell
adb -s emulator-5554 shell am instrument -w -r -e class com.mashudsck.dealghorithm.StateStoreTest com.mashudsck.dealghorithm.test/androidx.test.runner.AndroidJUnitRunner
```

The initial streamed install stalled. A task-created read-only emulator was
restarted without loading/saving snapshots; non-streaming installation succeeded.
An initial test signature failure was fixed before both tests passed. Ignored logs
are `build/foundation-final.log` and `build/storage-instrumentation.log`.

Packaged manifest inspection with SDK `aapt dump permissions` shows only the app's
generated non-exported-receiver permission. There is no INTERNET, accessibility,
overlay, exact-alarm or broad inventory permission. `aapt dump xmltree` confirms
`allowBackup=false` and both backup-rule references. Cloud and device-transfer
rules exclude every baseline data domain; state additionally uses noBackupFilesDir.
This verifies configuration and path, not an executed OEM device-transfer trial.

Lint reports zero errors and one existing missing-application-icon warning.
Manual intentions, activity UI, automatic monitoring and their acceptance gates
remain incomplete. No physical phone or automatic support is claimed.
