# PAICOT — Universal Android Control & Python Automation Platform

PAICOT is an Android app that embeds a real Python 3.11 interpreter
(Chaquopy) and exposes the Android device to Python scripts through a
native library (`import paicot`) plus a local authenticated HTTP/WebSocket
API. It runs without Termux.

## Quick start

- Install the APK (Debug build is signed with the standard debug key).
- Open PAICOT -> Permissions and grant what the scripts need:
  - **Accessibility** (PAICOT Automation service) — tap/swipe/type/find UI elements.
  - **Overlay** — floating panels (`paicot.display`).
  - **Notifications** — NotificationListener — `paicot.notifications`.
  - **Screen capture** — screenshot (`paicot.screen.screenshot()`).
  - **SAF folder** (Files screen) — user folders via Storage Access Framework.
- Terminal tab: write Python, tap Run. Example:

```python
import paicot

paicot.apps.open("com.android.settings")
paicot.sleep(2)
cx, cy = paicot.screen.center()
paicot.screen.tap(cx, cy)
paicot.keyboard.type("Hello PAICOT")
print(paicot.device.info())
```

More examples in `python/examples/` (welcome panel, wait-and-click,
notifications, files, list apps, open settings, tap & swipe, type text,
overlay panel, files rw).

## Architecture

See `docs/ARCHITECTURE.md`.

- `app/` — Kotlin app (Jetpack Compose UI, stdlib HTTP/WebSocket server,
  Accessibility/Notification listener services, Room + DataStore).
- `app/src/main/python/paicot/` — the Python library embedded via Chaquopy.
- `.github/workflows/` — CI builds the debug APK artifact
  (`PAICOT-Android-APK`) and optional signed releases.

## Modes

- **STANDARD** (default): works with user-granted permissions only.
- **ADVANCED**: adds the optional Shizuku integration point (reflection-based,
  no hard dependency).
- **ROOT**: reserved for future root-only APIs; STANDARD keeps working.

## Security model

- The local server binds `127.0.0.1` by default (port 7777). Binding to
  `0.0.0.0` requires an explicit Settings change and is on the user.
- Every `/api/v1/*` route except the health check requires
  `Authorization: Bearer <token>` (or `X-PAICOT-Token`). The token is shown
  in Settings and can be regenerated there. No request is executed without a
  valid token — never expose the server externally without that.

## HTTP API (base: http://127.0.0.1:7777)

| Method | Path                          | Auth | Body / Notes |
|--------|-------------------------------|------|--------------|
| GET    | /api/v1/server/status         | no   | health + bound port + python readiness |
| POST   | /api/v1/python/execute        | yes  | `{"code": str, "timeout_seconds": 30}` -> ExecutionResult JSON |
| POST   | /api/v1/python/cancel         | yes  | cooperative cancel of the running script |
| GET    | /api/v1/python/status         | yes  | runtime ready / running state |
| GET    | /api/v1/device/info           | yes  | DeviceInfo JSON |
| GET    | /api/v1/apps?include_system=1 | yes  | installed launcher apps |
| POST   | /api/v1/apps/open             | yes  | `{"package": "com.example.app"}` |
| POST   | /api/v1/screen/tap            | yes  | `{"x": 500, "y": 800}` |
| POST   | /api/v1/screen/swipe          | yes  | `{"x1":..,"y1":..,"x2":..,"y2":..,"duration":500}` |
| GET    | /api/v1/permissions           | yes  | permission diagnosis |
| WS     | /ws                           | —    | real-time log stream (JSON text frames) |

```bash
curl http://127.0.0.1:7777/api/v1/server/status
TOKEN=<token> curl -H "Authorization: Bearer $TOKEN" \
  -d '{"code":"print(1+1)"}' http://127.0.0.1:7777/api/v1/python/execute
```

## Building

- Local (needs Android SDK + JDK 17 + Python 3.11 for Chaquopy):
  `./gradlew assembleDebug` -> `app/build/outputs/apk/debug/app-debug.apk`
- CI: push to `main` or run the workflow manually; the APK is published
  as the `PAICOT-Android-APK` artifact.
- Signed release: create a keystore, then set the GitHub secrets
  `ANDROID_KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`
  and run the workflow with "build release" or push a `v*.*.*` tag.

## Tests

```bash
python3 -m unittest discover -s tests/python   # paicot library (desktop, fake bridge)
./gradlew testDebugUnitTest                      # JVM Auth/Validation/enum tests
```

## Design

Deep black (`#050505`) + graphite (`#1A1A1C`) + red accent (`#E53935`),
Material 3, vector icons, no emojis.
