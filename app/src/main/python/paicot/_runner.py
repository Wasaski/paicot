"""Script runner used by Kotlin's PythonRuntime.

Callable from both Chaquopy (on-device) and desktop CPython (tests).

run_code(code) -> {"stdout": str, "stderr": str, "result": str|None, "error": str|None}

If the last statement of `code` is an expression, its repr is returned as
`result` (mirrors the Python REPL).
"""

import ast
import io
import sys
import traceback
from contextlib import redirect_stdout, redirect_stderr


def run_code(code, filename="<paicot>"):
    out = io.StringIO()
    err = io.StringIO()
    result = {"stdout": "", "stderr": "", "result": None, "error": None}
    namespace = {"__name__": "__main__"}

    try:
        if not isinstance(code, str):
            raise TypeError(f"code must be str, got {type(code).__name__}")
        tree = ast.parse(code, filename=filename)
        last_expr = None
        if tree.body and isinstance(tree.body[-1], ast.Expr):
            last_expr = tree.body.pop()
            script_tree = ast.Module(body=tree.body, type_ignores=[])
            ast.fix_missing_locations(script_tree)
        else:
            script_tree = tree

        with redirect_stdout(out), redirect_stderr(err):
            if script_tree.body:
                exec(compile(script_tree, filename=filename, mode="exec"), namespace)
            if last_expr is not None:
                value = eval(compile(ast.Expression(last_expr.value), filename=filename, mode="eval"), namespace)
                if value is not None:
                    result["result"] = repr(value)

    except SyntaxError as exc:
        result["error"] = f"SyntaxError: {exc}"
    except BaseException as exc:  # noqa: BLE001 - scripts may raise anything
        msg = f"{type(exc).__name__}: {exc}"
        if "cancelled" in str(exc).lower():
            msg = "Cancelled: execution cancelled"
        result["error"] = msg
        err.write(traceback.format_exc())

    result["stdout"] = out.getvalue()
    result["stderr"] = err.getvalue()
    return result


if __name__ == "__main__":
    import json

    sample = sys.stdin.read()
    print(json.dumps(run_code(sample), indent=2))
