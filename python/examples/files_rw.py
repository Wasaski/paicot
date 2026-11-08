"""Read/write files in PAICOT's private root."""
import paicot

print(paicot.files.write("notes/hello.txt", "Written from Python"))
print(paicot.files.read("notes/hello.txt"))
print(paicot.files.list_entries(""))
print(paicot.files.list_entries("notes"))
