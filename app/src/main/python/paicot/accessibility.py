"""Accessibility Engine: read the active window tree and click elements."""

from ._bridge import call


def find(text):
    """All nodes whose text/description contains `text` (case-insensitive)."""
    return call("accessibility_find", str(text))


def click(text):
    """Click the first clickable element matching `text` (case-insensitive)."""
    return call("accessibility_click", str(text))
