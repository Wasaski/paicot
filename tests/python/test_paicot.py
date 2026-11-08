"""Desktop tests for the paicot Python library (no Android needed).

Uses a fake `paicot_bridge` injected into builtins, mirroring what Kotlin's
PythonRuntime does on-device. Runs with: python3 -m unittest discover -s tests/python
"""

import builtins
import json
import os
import sys
import unittest

REPO_ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
sys.path.insert(0, os.path.join(REPO_ROOT, "app", "src", "main", "python"))

import paicot  # noqa: E402
from paicot import _runner  # noqa: E402
from paicot._bridge import PaicotError  # noqa: E402


class FakeBridge:
    """Mirrors the Kotlin PaicotBridge JSON contract."""

    def __init__(self):
        self.calls = []
        self.cancel = False

    def ping(self):
        if self.cancel:
            raise RuntimeError("Cancelled")
        return "alive"

    def _ok(self, method, data):
        self.calls.append((method, data))
        return json.dumps({"ok": True, "data": data, "timestamp": 1})

    def _fail(self, message):
        return json.dumps({"ok": False, "error": message, "timestamp": 1})

    def apps_open(self, package_name):
        return self._ok("apps_open", {"opened": package_name})

    def apps_list(self, include_system):
        return self._ok("apps_list", {"apps": [], "count": 0})

    def screen_tap(self, x, y):
        if x < 0 or y < 0:
            return self._fail("negative coordinates")
        return self._ok("screen_tap", {"x": x, "y": y})

    def screen_swipe(self, x1, y1, x2, y2, duration):
        return self._ok("screen_swipe", {"from": f"{x1},{y1}", "to": f"{x2},{y2}"})

    def screen_size(self):
        return self._ok("screen_size", {"width_px": 1080, "height_px": 2400})

    def keyboard_type(self, text):
        return self._ok("keyboard_type", {"typed_chars": len(text)})

    def device_info(self):
        return self._ok("device_info", {"model": "TestPhone"})

    def server_status(self):
        return self._ok("server_status", {"running": True})

    def notifications_active(self):
        return self._ok("notifications_active", {"count": 0, "notifications": []})


class BridgeTest(unittest.TestCase):
    def setUp(self):
        self.fake = FakeBridge()
        builtins.paicot_bridge = self.fake

    def tearDown(self):
        if hasattr(builtins, "paicot_bridge"):
            delattr(builtins, "paicot_bridge")

    def test_import_modules(self):
        for mod in ("apps", "screen", "keyboard", "accessibility", "display",
                    "files", "notifications", "device", "automation", "server"):
            self.assertTrue(hasattr(paicot, mod), mod)

    def test_version(self):
        self.assertEqual(paicot.__version__, "0.1.0")

    def test_apps_open(self):
        data = paicot.apps.open("com.android.settings")
        self.assertEqual(data["opened"], "com.android.settings")

    def test_screen_positional_args(self):
        data = paicot.screen.tap(500, 800)
        self.assertEqual(data["x"], 500)
        self.assertEqual(data["y"], 800)

    def test_screen_error_raises(self):
        with self.assertRaises(PaicotError):
            paicot.screen.tap(-1, 10)

    def test_screen_center(self):
        cx, cy = paicot.screen.center()
        self.assertEqual((cx, cy), (540, 1200))

    def test_keyboard_type(self):
        data = paicot.keyboard.type_text("Hello PAICOT")
        self.assertEqual(data["typed_chars"], 12)

    def test_device_info(self):
        self.assertEqual(paicot.device.info()["model"], "TestPhone")

    def test_sleep_ok(self):
        paicot.sleep(0.05)  # must not raise

    def test_sleep_cancel(self):
        self.fake.cancel = True
        with self.assertRaises(RuntimeError):
            paicot.sleep(5)

    def test_no_bridge_raises_paicot_error(self):
        delattr(builtins, "paicot_bridge")
        with self.assertRaises(PaicotError):
            paicot.apps.open("com.example")


class RunnerTest(unittest.TestCase):
    def test_stdout(self):
        out = _runner.run_code("print('hello')")
        self.assertEqual(out["stdout"], "hello\n")
        self.assertIsNone(out["error"])

    def test_stderr(self):
        out = _runner.run_code("import sys; sys.stderr.write('oops')")
        self.assertEqual(out["stderr"], "oops")

    def test_exception(self):
        out = _runner.run_code("raise ValueError('bad')")
        self.assertIn("ValueError", out["error"])
        self.assertIn("bad", out["error"])
        self.assertIn("Traceback", out["stderr"])

    def test_syntax_error(self):
        out = _runner.run_code("def broken(:")
        self.assertIn("SyntaxError", out["error"])

    def test_last_expression_result(self):
        out = _runner.run_code("x = 40\nx + 2")
        self.assertEqual(out["result"], "42")

    def test_no_result(self):
        out = _runner.run_code("print('x')")
        self.assertIsNone(out["result"])

    def test_cancelled_maps(self):
        out = _runner.run_code("raise RuntimeError('Cancelled')")
        self.assertTrue(out["error"].startswith("Cancelled"), out["error"])

    def test_paicot_import_inside_script(self):
        out = _runner.run_code("import paicot\nprint(paicot.__version__)")
        self.assertIn("0.1.0", out["stdout"])

    def test_empty_code(self):
        out = _runner.run_code("")
        self.assertIsNone(out["error"])


if __name__ == "__main__":
    unittest.main()
