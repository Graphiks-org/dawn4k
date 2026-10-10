#!/usr/bin/env python3
"""Pointer-only qualification of the actual Compose panel in the isolated lab."""
import argparse
import json
import os
from pathlib import Path
import socket
import struct
import subprocess
import time

STATE = Path(os.environ.get("DAWN_UI_PROBE_STATE", "/workspace/dawn4k-demo/build/ui-qualification/state.json"))


def read_state(path=STATE):
    return json.loads(path.read_text())


def await_state(predicate, timeout=5):
    deadline = time.monotonic() + timeout
    last = None
    while time.monotonic() < deadline:
        try:
            last = read_state()
            if predicate(last):
                return last
        except FileNotFoundError:
            pass
        time.sleep(0.05)
    raise AssertionError(f"qualification state deadline exceeded: {last}")


def compositor_environment():
    return dict(os.environ, **json.loads(Path("/run/user/1000/desktop-env.json").read_text()))


def sway(command):
    result = subprocess.run(["swaymsg", *command], env=compositor_environment(),
        text=True, stdout=subprocess.PIPE, stderr=subprocess.PIPE, check=True, timeout=5)
    return json.loads(result.stdout)


def nodes(node):
    yield node
    for child in node.get("nodes", []) + node.get("floating_nodes", []):
        yield from nodes(child)


def window_for(pid):
    matches = [n for n in nodes(sway(["-t", "get_tree"])) if n.get("pid") == pid]
    if len(matches) != 1:
        raise AssertionError(f"expected one Compose application window, got {len(matches)}")
    return matches[0]


def read_exact(connection, count):
    output = bytearray()
    while len(output) < count:
        part = connection.recv(count - len(output))
        if not part:
            raise EOFError("VNC connection ended")
        output.extend(part)
    return bytes(output)


def pointer_click(x, y):
    # Same WayVNC input path as noVNC. This does not use X11 or alter client state.
    with socket.create_connection(("127.0.0.1", 5900), timeout=5) as connection:
        version = read_exact(connection, 12)
        if version != b"RFB 003.008\n":
            raise ValueError(f"unsupported RFB version {version!r}")
        connection.sendall(version)
        count = read_exact(connection, 1)[0]
        security = read_exact(connection, count)
        if 1 not in security:
            raise ValueError("qualification VNC requires localhost no-auth security")
        connection.sendall(b"\x01")
        if struct.unpack(">I", read_exact(connection, 4))[0] != 0:
            raise ValueError("VNC security negotiation failed")
        connection.sendall(b"\x01")  # Shared desktop, do not disconnect browser clients.
        header = read_exact(connection, 24)
        read_exact(connection, struct.unpack(">I", header[20:24])[0])
        connection.sendall(struct.pack(">BBHH", 5, 0, x, y))
        connection.sendall(struct.pack(">BBHH", 5, 1, x, y))
        connection.sendall(struct.pack(">BBHH", 5, 0, x, y))


def click_control(name):
    state = await_state(lambda s: name in s["bounds"])
    window = window_for(state["pid"])
    output = next(o for o in sway(["-t", "get_outputs"]) if o["name"] == "HEADLESS-1")
    scale = output["scale"]
    bounds = state["bounds"][name]
    # Compose's boundsInWindow are physical pixels; Sway's position is logical.
    x = round(window["rect"]["x"] * scale + bounds["x"] + bounds["width"] / 2)
    y = round(window["rect"]["y"] * scale + bounds["y"] + bounds["height"] / 2)
    pointer_click(x, y)


def qualify():
    result_path = STATE.with_name("result.json")
    result = {"composeNative": False, "pointerControls": False, "scale2": False, "parentApi": False}
    try:
        state = await_state(lambda s: s["ready"] and "Close" in s["bounds"], timeout=600)
        assert state["toolkit"] == "sun.awt.wl.WLToolkit", state
        window = window_for(state["pid"])
        assert window.get("shell") == "xdg_shell" and window.get("window") is None, window
        assert not compositor_environment().get("DISPLAY"), "qualification exports DISPLAY"
        assert subprocess.run(["pgrep", "Xwayland"], stdout=subprocess.DEVNULL).returncode == 1
        result["composeNative"] = True
        for scale in (1, 2, 1):
            response = sway([f"output HEADLESS-1 scale {scale}"])
            assert all(r.get("success") for r in response), response
            # Record a post-layout state before acting on scale-sensitive bounds.
            time.sleep(0.3)
            current = read_state()
            click_control("Resume" if current["paused"] else "Pause")
            await_state(lambda s: s["paused"] != current["paused"])
            click_control("256")
            await_state(lambda s: s["count"] == 256)
            previous = read_state()["resetGeneration"]
            click_control("Reset")
            await_state(lambda s: s["resetGeneration"] == previous + 1)
            if scale == 1:
                result["pointerControls"] = True
            else:
                result["scale2"] = True
        state = read_state()
        result["parentApi"] = state["parent"].get("ready", False)
        STATE.with_name("parent-contract.json").write_text(json.dumps(state["parent"], indent=2))
        assert result["parentApi"], state["parent"]
        click_control("Close")
        deadline = time.monotonic() + 10
        while time.monotonic() < deadline:
            try:
                window_for(state["pid"])
            except AssertionError:
                break
            time.sleep(0.05)
        else:
            raise AssertionError("Close did not dispose the native Compose window")
        print(json.dumps(result, indent=2))
    except BaseException as failure:
        result["failure"] = str(failure)
        raise
    finally:
        result_path.parent.mkdir(parents=True, exist_ok=True)
        result_path.write_text(json.dumps(result, indent=2))
        sway(["output HEADLESS-1 scale 1"])


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--click")
    args = parser.parse_args()
    if args.click:
        click_control(args.click)
    else:
        qualify()
