"""Display Engine: floating panels over other apps (TYPE_APPLICATION_OVERLAY)."""

from ._bridge import call


def show_panel(title, body="", x=60, y=200):
    return call("display_show", str(title), str(body), int(x), int(y))


def hide_panel():
    return call("display_hide")
