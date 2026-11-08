"""List installed launcher apps."""
import paicot

apps = paicot.apps.list_all()["apps"]
for app in apps[:30]:
    print(f"{app['label']:<28} {app['package']}")
print(f"... {len(apps)} apps total")
