"""PAICOT scripting library.

import paicot

paicot.apps.open("com.android.settings")
paicot.sleep(2)
paicot.screen.tap(500, 800)
paicot.keyboard.type("Hello PAICOT")
info = paicot.device.info()

All functions are thin wrappers over the Kotlin bridge (see _bridge.py).
Outside the PAICOT app (desktop tests) the bridge is unavailable and calls
raise PaicotError — imports always succeed.
"""

__version__ = "0.1.0"

from . import (
    apps,
    screen,
    keyboard,
    accessibility,
    display,
    files,
    notifications,
    device,
    automation,
    server,
)
from ._bridge import PaicotError, call as _bridge_call, ping as _bridge_ping
import time as _time


def sleep(seconds):
    """Sleep, checking the cooperative cancel flag every 100ms."""
    seconds = float(seconds)
    if seconds < 0:
        raise ValueError("seconds must be >= 0")
    deadline = _time.monotonic() + seconds
    while True:
        _bridge_ping()
        remaining = deadline - _time.monotonic()
        if remaining <= 0:
            return
        _time.sleep(min(0.1, remaining))
