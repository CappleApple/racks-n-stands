"""Small local validation client; reads credentials from the selected development server."""
import argparse
import json
from pathlib import Path
import socket
import struct

parser = argparse.ArgumentParser()
parser.add_argument("commands", nargs="*")
parser.add_argument("--server", default="run-server")
parser.add_argument("--file")
args = parser.parse_args()
if args.file:
    args.commands += json.loads(Path(args.file).read_text())
properties = dict(line.split("=", 1) for line in (Path(args.server) / "server.properties").read_text().splitlines() if "=" in line and not line.startswith("#"))

def exact(sock, size):
    result = b""
    while len(result) < size:
        chunk = sock.recv(size - len(result))
        if not chunk:
            raise ConnectionError("Server closed RCON connection")
        result += chunk
    return result

def receive(sock):
    size, = struct.unpack("<i", exact(sock, 4))
    packet = exact(sock, size)
    request_id, kind = struct.unpack("<ii", packet[:8])
    return request_id, kind, packet[8:-2].decode("utf-8")

def send(sock, request_id, kind, text):
    data = struct.pack("<ii", request_id, kind) + text.encode() + b"\0\0"
    sock.sendall(struct.pack("<i", len(data)) + data)

with socket.create_connection(("127.0.0.1", int(properties.get("rcon.port", "25588"))), timeout=20) as connection:
    send(connection, 1, 3, properties["rcon.password"])
    request_id, _, _ = receive(connection)
    if request_id == -1:
        raise PermissionError("RCON authentication failed")
    for index, command in enumerate(args.commands, 2):
        send(connection, index, 2, command)
        _, _, response = receive(connection)
        print(f"> {command}\n{response}")
