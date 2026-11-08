"""Keyboard: text insertion and navigation keys.

Insertion uses the clipboard + ACTION_PASTE on the focused node (restores the
previous clipboard contents). Raw keycode injection needs an IME/root and is
reported as unsupported instead of faked.
"""

from ._bridge import call


def type_text(text):
    return call("keyboard_type", str(text))


def press(key):
    """key: back | home | recents | notifications | quick_settings | power_dialog"""
    return call("keyboard_key", str(key))
