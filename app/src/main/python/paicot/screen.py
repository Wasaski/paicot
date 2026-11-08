"""Screen Control: taps, swipes, long-press, geometry, screenshots.

Gestures go through the AccessibilityService (dispatchGesture); they need the
service enabled in Permissions. Screenshot needs the MediaProjection consent.
"""

from ._bridge import call


def tap(x, y):
    return call("screen_tap", int(x), int(y))


def swipe(x1, y1, x2, y2, duration=500):
    return call("screen_swipe", int(x1), int(y1), int(x2), int(y2), int(duration))


def long_press(x, y, duration=800):
    return call("screen_long_press", int(x), int(y), int(duration))


def size():
    return call("screen_size")


def screenshot():
    """Return {'format', 'width_px', 'height_px', 'base64'} (PNG)."""
    return call("screen_screenshot")


def center():
    """Center point of the display, handy for taps."""
    s = size()
    return (s["width_px"] // 2, s["height_px"] // 2)
