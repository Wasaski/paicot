"""Wait for a UI element to appear, then click it."""
import paicot

paicot.apps.open("com.android.settings")
paicot.sleep(2)
found = paicot.automation.wait_for("About", timeout=10)
print(found)
if found:
    print(paicot.accessibility.click("About"))
