"""Notification Engine: active notifications via NotificationListenerService."""

from ._bridge import call


def active():
    """{'count': N, 'notifications': [{'package', 'title', 'text', 'posted_at'}...]}"""
    return call("notifications_active")


def from_package(package_name):
    return [n for n in active()["notifications"] if n["package"] == package_name]
