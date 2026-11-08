"""Device Manager: read-only device facts."""

from ._bridge import call


def info():
    return call("device_info")


def network():
    return call("device_network")
