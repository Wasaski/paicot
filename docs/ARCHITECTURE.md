# PAICOT — Architecture

## Layout

```
paicot/
├── app/
│   ├── build.gradle.kts
│   ├── src/main/
│   │   ├── AndroidManifest.xml
│   │   ├── java/com/paicot/
│   │   │   ├── MainActivity.kt            # single-activity Compose host, SAF + capture launchers
│   │   │   ├── PaicotApp.kt               # composition root (object Paicot)
│   │   │   ├── core/                      # models, DataStore prefs, in-memory logger
│   │   │   ├── server/                    # HTTP/WebSocket server + foreground service
│   │   │   ├── python/                    # Chaquopy runtime wrapper (executor, timeout, cancel)
│   │   │   ├── bridge/                    # Kotlin<->Python bridge (JSON envelopes)
│   │   │   ├── automation/                # script runs, recent-runs history, cancel
│   │   │   ├── accessibility/             # AccessibilityService: gestures + node tree
│   │   │   ├── applications/              # PackageManager listing + intents
│   │   │   ├── screen/                    # gestures via service, size, MediaProjection screenshot
│   │   │   ├── keyboard/                  # clipboard + ACTION_PASTE typing, global keys
│   │   │   ├── display/                   # TYPE_APPLICATION_OVERLAY panels
│   │   │   ├── storage/                   # app-private files + SAF tree (DocumentsContract)
│   │   │   ├── notifications/             # NotificationListenerService + collector
│   │   │   ├── device/                    # Build/metrics/battery/network facts
│   │   │   ├── permissions/               # diagnosis + settings intents
│   │   │   ├── database/                  # Room: scripts + execution history
│   │   │   └── ui/                        # theme, root, 7 screens, UiStore (state)
│   │   ├── python/paicot/                 # embedded Python library (Chaquopy source dir)
│   │   └── res/
├── python/examples/                       # runnable example scripts
├── tests/python/                          # desktop unittest suite (fake bridge)
├── .github/workflows/                     # android-build.yml, release.yml
└── docs/
```

## Python engine (Chaquopy)

- `Python.start(AndroidPlatform(context))` once at app start; the bridge
  object is injected into `builtins.picot_bridge` so Python code resolved by
  `import paicot` calls Kotlin directly.
- Scripts run on a dedicated single thread (`paicot-python`).
  `paicot._runner.run_code` executes them with `redirect_stdout/stderr`,
  returns a structured dict, and (REPL-style) returns the repr of a final
  expression. Kotlin interprets it into `ExecutionResult`.
- Timeout: `Future.get(timeout)` marks TIMED_OUT and sets the cooperative
  cancel flag. Cancel is cooperative — `paicot.sleep()` polls
  `paicot_bridge.ping()` every 100 ms and every bridge method call checks it.
  Python threads cannot be force-killed safely, so a timed-out thread is
  abandoned (documented limitation).
- History: every execution is persisted to Room (`executions`), capped at
  100 rows.

## Bridge contract

Kotlin methods are snake_case and return JSON strings:

`{"ok": true, "data": {...}}` or `{"ok": false, "error": "..."}`

The Python side (`paicot._bridge.call`) parses the envelope, returns `data`
or raises `PaicotError`. Arguments are always positional — Chaquopy
keyword-argument fidelity for Java methods is not guaranteed.

## Local server

- Transport: stdlib `java.net.ServerSocket`. Ktor server engines target JVM
  hosts, not Android; the transport surface (routes, JSON envelopes, WS
  frames) is kept small so a Ktor CIO swap is possible without touching
  clients. Responses are written as exact byte-length payloads over the raw
  `OutputStream` (encoding is fixed to UTF-8/ISO-8859-1).
- Auth: bearer token from DataStore on every `/api/v1/*` route except
  `GET /api/v1/server/status` (health requires no token). Comparison is
  constant-time. Request size caps: 64 KB headers, 2 MB body, 200 KB script.
- WebSocket (`/ws`, RFC 6455): server pushes log entries as JSON text frames
  via a `SharedFlow` fan-out; client ping/close frames are handled.
- `PaicotForegroundService` keeps the server alive in background and surfaces
  the bound port / bind error in its notification.

## Permissions & privilege modes

- STANDARD: everything goes through user-granted permissions (Accessibility,
  Overlay, Notification listener, MediaProjection, SAF, POST_NOTIFICATIONS).
- ADVANCED: reserved for the optional Shizuku integration point
  (reflection-based, no compile-time dependency).
- ROOT: reserved; nothing in STANDARD mode needs root.

Android sandboxing is respected: files outside the granted SAF tree are never
read directly (scoped storage on Android 11+).
