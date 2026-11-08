"""App Manager: list and open installed applications."""

from ._bridge import call


def open(package_name):
    """Open a launcher app by package name."""
    return call("apps_open", str(package_name))


def list_all(include_system=False):
    """List launcher apps visible to PAICOT (respecting Android visibility rules)."""
    return call("apps_list", bool(include_system))


def info(package_name):
    """Label/system info for a package."""
    return call("apps_info", str(package_name))
