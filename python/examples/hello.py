"""Hello, PAICOT: device info + server status."""
import paicot

print("PAICOT version:", paicot.__version__)
info = paicot.device.info()
print(info)
print("Server:", paicot.server.status())
