#!/usr/bin/env python3
"""Run as a Sway child to export its actual XWayland/Wayland environment."""
import json
import os
from pathlib import Path

runtime = Path(os.environ["XDG_RUNTIME_DIR"])
temporary = runtime / f"desktop-env-{os.getpid()}.json"
keys = ("DISPLAY", "WAYLAND_DISPLAY", "SWAYSOCK", "XDG_RUNTIME_DIR",
        "DBUS_SESSION_BUS_ADDRESS", "XDG_SESSION_TYPE")
temporary.write_text(json.dumps({key: os.environ[key] for key in keys if key in os.environ}))
temporary.chmod(0o600)
temporary.replace(runtime / "desktop-env.json")
