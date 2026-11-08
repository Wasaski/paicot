"""Bridge to Kotlin. The Kotlin side injects `paicot_bridge` into builtins
when the Chaquopy runtime starts; every call returns a JSON string:

    {"ok": true, "data": {...}}  or  {"ok": false, "error": "..."}

Args are always passed positionally (Chaquopy keyword-arg fidelity for Java
methods is not guaranteed).
"""

import builtins
import json


class PaicotError(Exception):
    pass


def _get_bridge():
    return getattr(builtins, "paicot_bridge", None)


def available():
    return _get_bridge() is not None


def ping():
    bridge = _get_bridge()
    if bridge is None:
        return True  # nothing to cancel against outside the app
    bridge.ping()  # raises RuntimeError("Cancelled") when cancel is requested
    return True


def call(method, *args):
    bridge = _get_bridge()
    if bridge is None:
        raise PaicotError(
            "Kotlin bridge unavailable — run inside the PAICOT app "
            f"(method: {method})"
        )
    fn = getattr(bridge, method, None)
    if fn is None:
        raise PaicotError(f"Bridge method not found: {method}")
    raw = fn(*args)
    try:
        payload = json.loads(raw)
    except Exception as exc:
        raise PaicotError(f"Invalid bridge response for {method}: {raw!r}") from exc
    if not payload.get("ok"):
        raise PaicotError(payload.get("error", "unknown bridge error"))
    return payload.get("data", {})
