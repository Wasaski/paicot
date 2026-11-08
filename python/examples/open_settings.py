"""Open the Android Settings app."""
import paicot

print(paicot.apps.open("com.android.settings"))
paicot.sleep(1)
print("Opened Settings")
