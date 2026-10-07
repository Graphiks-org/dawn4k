"""Backend-specific compositor environment; no connection probes or side effects."""


def validate_desktop_environment(environment, backend):
    if backend not in ("x11", "wayland"):
        raise RuntimeError(f"unknown desktop backend: {backend}")
    result = dict(environment)
    if backend == "x11" and not result.get("DISPLAY", "").strip():
        raise RuntimeError("missing DISPLAY; wait for the X11 desktop to become healthy")
    if not result.get("WAYLAND_DISPLAY", "").strip():
        raise RuntimeError("missing WAYLAND_DISPLAY; wait for the compositor to become healthy")
    if not result.get("XDG_RUNTIME_DIR", "").strip():
        raise RuntimeError("missing XDG_RUNTIME_DIR for Wayland")
    # Exported compositor sockets are authoritative, not an inherited anonymous fd.
    result.pop("WAYLAND_SOCKET", None)
    if backend == "wayland":
        for key in ("DISPLAY", "XAUTHORITY", "_JAVA_AWT_WM_NONREPARENTING"):
            result.pop(key, None)
    return result
