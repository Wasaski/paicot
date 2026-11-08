"""Show active notifications (requires notification listener enabled)."""
import paicot

data = paicot.notifications.active()
print(f"{data['count']} active")
for n in data["notifications"]:
    print(f"- {n['package']}: {n['title']} {n['text']}")
