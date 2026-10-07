#!/usr/bin/env python3
"""Run as a Sway child to export its actual XWayland/Wayland environment."""
import json
import os
from pathlib import Path
from desktop_environment import validate_desktop_environment

runtime = Path(os.environ["XDG_RUNTIME_DIR"])
temporary = runtime / f"desktop-env-{os.getpid()}.json"
keys = ("DISPLAY", "WAYLAND_DISPLAY", "SWAYSOCK", "XDG_RUNTIME_DIR",
        "DBUS_SESSION_BUS_ADDRESS", "XDG_SESSION_TYPE")
environment = validate_desktop_environment(os.environ, os.environ.get("DAWN_DESKTOP_BACKEND", "x11"))
temporary.write_text(json.dumps({key: environment[key] for key in keys if key in environment}))
temporary.chmod(0o600)
temporary.replace(runtime / "desktop-env.json")
