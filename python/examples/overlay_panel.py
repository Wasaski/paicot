"""Show a floating panel over the current app for 5 seconds."""
import paicot

print(paicot.display.show_panel("PAICOT", "Overlay panel from Python"))
paicot.sleep(5)
print(paicot.display.hide_panel())
