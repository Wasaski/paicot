"""File Manager (app-private root + SAF granted tree).

Paths are relative to PAICOT's private root; `..` segments are rejected
Kotlin-side. Use the Files screen's SAF grant for user folders.
"""

from ._bridge import call


def read(path):
    return call("files_read", str(path))


def write(path, content):
    return call("files_write", str(path), str(content))


def list_entries(path=""):
    return call("files_list", str(path))
