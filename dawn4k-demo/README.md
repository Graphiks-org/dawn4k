# Dawn particle demo — native Wayland and X11

## Start and view: native Wayland

Use an ARM64 Mac with Docker Desktop running and several GB of free Docker disk
space. Network access is required for the pinned JDK/Dawn archives, Gradle, and
the project's Maven SNAPSHOT dependencies. No host JDK or Linux GPU is needed.

Docker Compose **2.24.4 or newer** is required for the Wayland configuration's
`!override` port replacement. From the repository root:

```bash
docker compose -f dawn4k-demo/docker/compose.yaml -f dawn4k-demo/docker/compose.wayland.yaml up --build -d --wait
docker compose -f dawn4k-demo/docker/compose.yaml -f dawn4k-demo/docker/compose.wayland.yaml exec desktop /opt/demo/demo.sh
```

Open <http://localhost:6081/vnc.html?autoconnect=1&shared=1&resize=scale>.
This is a **native Wayland window and Dawn/Vulkan surface**: Sway runs with
XWayland disabled, and the demo has no `DISPLAY` and initializes no Compose/AWT
or Xlib host. It uses the same particle renderer as the X11 demo.

Focus the particle window, then use:

| Key | Action |
| --- | --- |
| Space | Pause/resume the simulation |
| R | Reset particles |
| + / − | Next/previous supported particle count, clamped to device limits |
| Escape | Close the demo, leaving the desktop running |

`Alt` + left-drag moves the window; `Alt` + right-drag resizes it. Integer output
scales are supported; fractional scaling and a graphical Wayland control panel
are not implemented. The current count/state and shortcuts are in the window
title and logs (the minimal Sway pixel border does not display a title bar).

The Wayland desktop is project `dawn4k-wayland-desktop`; the X11 desktop below is
`dawn4k-linux-desktop`. Each has its own ten cache/output volumes, so both can run
together without overwriting builds. Both viewers show a shared desktop to all
connected browser clients. Rendering uses CPU lavapipe, **not the Mac GPU**.
Endpoints are localhost-only, unencrypted and unauthenticated: never publish them
on a public interface. The first launch can take several minutes with empty caches.

Use both `-f` arguments on every Wayland command:

```bash
# Services and current launch output
docker compose -f dawn4k-demo/docker/compose.yaml -f dawn4k-demo/docker/compose.wayland.yaml logs desktop
docker compose -f dawn4k-demo/docker/compose.yaml -f dawn4k-demo/docker/compose.wayland.yaml exec desktop tail -n 100 /workspace/dawn4k-demo/build/linux-desktop/demo.log
# Stop only this desktop, retaining its caches
docker compose -f dawn4k-demo/docker/compose.yaml -f dawn4k-demo/docker/compose.wayland.yaml down
```

Set `DAWN_WAYLAND_PORT` to change the default 6081 port; use a distinct Compose
project name for a separate checkout, and keep it consistent on subsequent commands.

## Automatic Linux backend selection

Ordinary application launches require **no platform flag**:
1. A nonblank `WAYLAND_DISPLAY` or inherited `WAYLAND_SOCKET` selects native Wayland.
2. Otherwise a nonblank `DISPLAY` selects the existing X11/Compose path.
3. Neither produces a diagnostic before any AWT initialization.

An advertised but unusable Wayland connection is an error: there is **no silent
X11 fallback**, even when `DISPLAY` also exists. Optional Linux-only overrides are
available for explicit regression testing and troubleshooting. Inside a native
Linux session:

```bash
./gradlew :dawn4k-demo:run --args=--platform=wayland
./gradlew :dawn4k-demo:run --args=--platform=x11
# After installDist, use the same application arguments:
dawn4k-demo/build/install/dawn4k-demo/bin/dawn4k-demo --platform=wayland
dawn4k-demo/build/install/dawn4k-demo/bin/dawn4k-demo --platform=x11
```

Manual runs need the real compositor environment; the Docker launcher reads it
from `/run/user/1000/desktop-env.json`. The X11 lab's default launcher explicitly
forces X11 because its Sway session advertises both protocols. The Wayland lab
uses automatic detection. macOS/Metal and Windows/D3D12 retain their existing path.

### Native bridge dependencies and packaging

Linux builds need JDK 25, a C compiler, `pkg-config`, `libwayland-dev`,
`wayland-protocols`/`wayland-scanner` and `libxkbcommon-dev`. Runtime needs
`libwayland-client.so.0`, `libxkbcommon.so.0`, Vulkan and a suitable driver.
The Docker image supplies them. Rebuild the image when upgrading an older X11
desktop: its previous image lacks the newly required build dependencies.

Gradle builds and packages `libdawn4k_wayland.so` for the Linux host architecture.
It does not require a Linux C compiler for macOS tests. To build a combined bridge
resource jar on the ARM64 Docker build image, use:

```bash
docker compose -f dawn4k-demo/docker/compose.yaml -f dawn4k-demo/docker/compose.wayland.yaml exec desktop /opt/demo/demo.sh :dawn4k-demo:jar -Pwayland.targets=linuxArm64,linuxX64
```

The x64 bridge is cross-compiled against the image's authenticated Ubuntu sysroot;
the existing ARM64 libraries are not replaced. ARM64 native presentation and both
bridge compilations were validated; Linux x64 and Windows native presentation
were not executed during this extension. macOS JVM tests/ABI checks were run.

## Start and view: X11 / XWayland (retained)

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
`DAWN_DESKTOP_PORT=6083` for another checkout running at the same time.
Use the same project name and port on every command for that instance:

```bash
DAWN_DESKTOP_PORT=6083 docker compose -p dawn4k-other -f dawn4k-demo/docker/compose.yaml up --build -d --wait
DAWN_DESKTOP_PORT=6083 docker compose -p dawn4k-other -f dawn4k-demo/docker/compose.yaml exec desktop /opt/demo/demo.sh
```

Its viewer URL uses port 6083. Volumes remain after `down`; another project name
has separate caches and outputs. Volume mounts use `nocopy` so a fresh Linux
environment does not inherit the checkout's macOS build outputs.

## Tests

Native Wayland validation (no X11):

```bash
docker compose -f dawn4k-demo/docker/compose.yaml -f dawn4k-demo/docker/compose.wayland.yaml exec -e DAWN_WAYLAND_TESTS=1 desktop /opt/demo/demo.sh :dawn4k-demo:test :dawn4k:jvmTest :dawn4k-native:jvmTest :dawn4k-native:verifyDawnAbi
python3 scripts/test-wayland-desktop-opt-in.py
python3 scripts/test-wayland-demo-entrypoint.py
# Starts/stops only a disposable project on 6082 for compositor-loss testing
DAWN_WAYLAND_DISCONNECT_TESTS=1 python3 scripts/test-wayland-demo-entrypoint.py
```

This exercises a real surface, particle rendering, keyboard controls, native
resize, integer scale changes, close during startup, Escape and compositor close.
The native C tests include protocol failure fixtures, keymaps, cleanup and an ABI
oracle. Both desktop opt-ins force execution instead of cached/up-to-date results;
without the relevant opt-in, native test bodies are guarded out.

X11 regression and shared packaging/supervisor checks:

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
# Actual standalone application rejects missing Wayland and X11 endpoints before AWT
python3 scripts/test-linux-demo-entrypoint.py
```

To use a separately rebuilt X11 regression project, set
`DAWN_X11_TEST_PROJECT=your-project` for both X11 Python scripts. Do not enable
`DAWN_DESKTOP_TESTS` in the Wayland-only lab, or `DAWN_WAYLAND_TESTS` in the X11 lab.

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

- **Missing/stale Wayland socket:** load the exported compositor environment and
  run as `demo`. Native Wayland requires a valid `XDG_RUNTIME_DIR` and the selected
  socket (or a genuinely inherited Wayland fd); an environment variable alone
  does not prove connectivity. There is no automatic X11 fallback.
- **Missing Wayland bridge/compiler/protocol XML:** rebuild the Docker image or
  install the native build dependencies above. Use a distribution built for your
  JVM's architecture; the bridge is extracted from its packaged resource into a
  private temporary directory. Check `ldd` for missing runtime dependencies.

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
  also checks Sway, the selected display protocol, and the VNC listener.
- **Maven SNAPSHOT dependency unavailable:** inspect the demo log and retry when
  the configured repository is reachable. The desktop remains usable even if
  the project cannot currently resolve its application dependencies.
- **Port already in use:** choose another loopback port and Compose project
  name as above; do not bind publicly to work around it.

The assistant can inspect browser screenshots of the same desktop. noVNC's
Linux application controls are pixels inside a canvas, not HTML buttons; where
browser automation cannot address them, scoped `xdotool` input for X11 or `wtype`
and Sway IPC for Wayland inside the container can exercise the visible UI.
You can use the mouse/keyboard normally
in your browser.
