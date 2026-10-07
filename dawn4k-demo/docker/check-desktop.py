#!/usr/bin/env python3
"""HTTP alone is not readiness: also connect to the compositor and XWayland."""
import json
import os
from pathlib import Path
import socket
import subprocess
import sys
from urllib.request import urlopen

try:
    environment = dict(os.environ, **json.loads(Path("/run/user/1000/desktop-env.json").read_text()))
    subprocess.run(["swaymsg", "-s", environment["SWAYSOCK"], "-t", "get_outputs"],
                   env=environment, check=True, timeout=2, stdout=subprocess.DEVNULL)
    subprocess.run(["xdpyinfo", "-display", environment["DISPLAY"]],
                   env=environment, check=True, timeout=2, stdout=subprocess.DEVNULL)
    with socket.create_connection(("127.0.0.1", 5900), timeout=2) as connection:
        if not connection.recv(12).startswith(b"RFB "):
            raise RuntimeError("wayvnc did not provide a VNC greeting")
    with urlopen("http://127.0.0.1:6080/vnc.html", timeout=2) as response:
        if response.status != 200:
            raise RuntimeError("noVNC endpoint is unavailable")
except Exception as failure:
    print(f"[desktop health] {failure}", file=sys.stderr)
    sys.exit(1)
