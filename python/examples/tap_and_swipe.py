"""Tap the centre of the screen, then swipe up."""
import paicot

cx, cy = paicot.screen.center()
print("Center:", cx, cy)
print(paicot.screen.tap(cx, cy))
paicot.sleep(1)
w, h = paicot.screen.size()["width_px"], paicot.screen.size()["height_px"]
print(paicot.screen.swipe(w // 2, int(h * 0.8), w // 2, int(h * 0.2), duration=600))
