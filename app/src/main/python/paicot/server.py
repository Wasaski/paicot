"""Server introspection (local API state as seen by the app)."""

from ._bridge import call


def status():
    """Server running state, host/port and Python engine readiness."""
    return call("server_status")
