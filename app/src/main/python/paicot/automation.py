"""Automation helpers: retry/wait utilities composed over the other modules."""

import time

from . import accessibility


def wait_for(text, timeout=10.0, interval=0.5):
    """Poll the accessibility tree until `text` appears (or the timeout hits).

    Returns the find() payload, or None on timeout.
    """
    deadline = time.monotonic() + timeout
    while True:
        found = accessibility.find(text)
        if found.get("elements"):
            return found
        if time.monotonic() > deadline:
            return None
        # paicot.sleep checks the cancel flag; here we just poll.
        time.sleep(interval)


def retry(fn, attempts=3, delay=0.5):
    """Call fn until it succeeds (no exception), or give up after `attempts`."""
    last = None
    for i in range(max(1, int(attempts))):
        try:
            return fn()
        except Exception as exc:  # noqa: BLE001
            last = exc
            if i < attempts - 1:
                time.sleep(delay)
    raise last
