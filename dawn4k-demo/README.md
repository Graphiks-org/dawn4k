# Dawn particle demo — Linux desktop

## Start and view

Use an ARM64 Mac with Docker Desktop running and several GB of free Docker disk
space. Network access is required for the pinned JDK/Dawn archives, Gradle, and
the project's Maven SNAPSHOT dependencies. No host JDK or Linux GPU is needed.

From the repository root:

```bash
docker compose -f dawn4k-demo/docker/compose.yaml up --build -d --wait
docker compose -f dawn4k-demo/docker/compose.yaml exec desktop /opt/demo/demo.sh
```

Open <http://localhost:6080/vnc.html?autoconnect=1&shared=1&resize=scale>.
All browser clients view the same desktop. The desktop can be started without
building the demo; the first demo launch downloads Gradle and Maven dependencies.
Click Pause/Resume, Reset, or a particle count in the demo. Close stops the demo
without stopping the desktop. `Alt` + left-drag moves floating windows; `Alt` +
right-drag resizes them. The browser scales a fixed 1280×800 Linux display.

Sway is Wayland, but Compose/AWT and the Dawn viewport use X11 through XWayland.
Vulkan uses Mesa lavapipe on the CPU, not the Mac GPU. This environment validates
functionality, not GPU performance. The web endpoint is local-only and has no
authentication: do not publish it on a public interface.

Desktop services and the demo run as the non-root `demo` user. The short root
entry point only initializes the named volume mount roots and private runtime
directory. It does not change source permissions. Sway uses floating demo
windows, and the image sets `_JAVA_AWT_WM_NONREPARENTING=1` so AWT responds to
real window-manager resizing, not just Java `setSize()` calls.

## Terminal, logs, stop, and rebuild

```bash
# Terminal; run /opt/demo/demo.sh from here too.
docker compose -f dawn4k-demo/docker/compose.yaml exec -u demo desktop bash
# Logs
docker compose -f dawn4k-demo/docker/compose.yaml logs desktop
# Last demo/build launch log (inside the Linux output volume)
docker compose -f dawn4k-demo/docker/compose.yaml exec desktop tail -n 100 /workspace/dawn4k-demo/build/linux-desktop/demo.log
# Stop without deleting Gradle/build caches
docker compose -f dawn4k-demo/docker/compose.yaml down
# Rebuild after changing Docker files, then wait for readiness
docker compose -f dawn4k-demo/docker/compose.yaml up --build -d --wait
```

The checkout is mounted read/write. Linux build outputs and caches are separate
named volumes, not the Mac's build directories. Use `-p another-name` and
`DAWN_DESKTOP_PORT=6081` for another checkout running at the same time.
Use the same project name and port on every command for that instance:

```bash
DAWN_DESKTOP_PORT=6081 docker compose -p dawn4k-other -f dawn4k-demo/docker/compose.yaml up --build -d --wait
DAWN_DESKTOP_PORT=6081 docker compose -p dawn4k-other -f dawn4k-demo/docker/compose.yaml exec desktop /opt/demo/demo.sh
```

Its viewer URL uses port 6081. Volumes remain after `down`; another project name
has separate caches and outputs. Volume mounts use `nocopy` so a fresh Linux
environment does not inherit the checkout's macOS build outputs.

## Tests

```bash
# Host-side supervisor, process shutdown, and launcher error handling
python3 -m unittest discover -s dawn4k-demo/docker -p 'test_*.py' -v
# Host-side real Gradle ARM64 archive/staging/JAR verification
python3 scripts/test-linux-arm64-packaging.py
# Actual Linux windows, Vulkan presentation, controls, Java/native resize,
# zero-sized viewport recovery, parent destruction, and startup cancellation
docker compose -f dawn4k-demo/docker/compose.yaml exec -e DAWN_DESKTOP_TESTS=1 desktop /opt/demo/demo.sh :dawn4k-demo:test :dawn4k:jvmTest :dawn4k-native:jvmTest :dawn4k-native:verifyDawnAbi
# Proves a dry/gated run cannot stand in for an opted-in desktop run
python3 scripts/test-linux-desktop-opt-in.py
# Actual standalone application rejects missing DISPLAY before initializing AWT
python3 scripts/test-linux-demo-entrypoint.py
```

Without `DAWN_DESKTOP_TESTS=1`, Linux display tests return before creating native
windows. With it, missing DISPLAY is a test failure, and Gradle always executes
the live desktop tests rather than accepting cached/up-to-date results.
The visibility tests also unmap a nonempty X11 parent: presentation stops while
hidden, the child is explicitly unmapped, and rendering resumes after restoration.

The launcher reads the actual display environment exported by Sway and selects
the installed lavapipe ICD by filename. It passes `-Pdawn.targets=linuxArm64`,
JDK/native-access settings come from the existing Gradle application/test
configuration, and the pinned Dawn resource loader verifies the extracted
library's content hash.

## Troubleshooting

- **APT reports invalid signatures:** first check Docker's free disk space.
  A full Docker disk can truncate downloads and cause misleading signature
  errors. Increase its disk limit or deliberately clean unused build cache;
  do not disable package signature verification.
- **Missing DISPLAY / XOpenDisplay / authorization:** use `/opt/demo/demo.sh`
  rather than invoking Gradle directly. For manual X11 tools, run as `demo` and
  load `/run/user/1000/desktop-env.json`; root is not authorized to the desktop's
  X server by default.
- **Missing/ambiguous lavapipe ICD:** rebuild the image and inspect
  `/usr/share/vulkan/icd.d/lvp*.json`. The launcher refuses to pick an arbitrary
  hardware driver.
- **Native library fails to load:** inspect `ldd` on
  `/workspace/dawn4k-native/build/native/linuxArm64/shared/lib/libwebgpu_dawn.so`.
  The JVM resource bundle is ARM64; an x64 JVM/image is not interchangeable.
- **Viewer opens but desktop is blank:** inspect `logs desktop`, check service
  health, and reconnect noVNC. HTTP readiness alone is insufficient; health
  also checks Sway, XWayland, and the VNC listener.
- **Maven SNAPSHOT dependency unavailable:** inspect the demo log and retry when
  the configured repository is reachable. The desktop remains usable even if
  the project cannot currently resolve its application dependencies.
- **Port already in use:** choose another loopback port and Compose project
  name as above; do not bind publicly to work around it.

The assistant can inspect browser screenshots of the same desktop. noVNC's
Linux application controls are pixels inside a canvas, not HTML buttons; where
browser automation cannot address them, scoped `xdotool` input inside the
container can exercise the visible UI. You can use the mouse/keyboard normally
in your browser.
