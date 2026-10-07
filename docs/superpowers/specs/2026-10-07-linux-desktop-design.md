# Linux demo in a browser-accessible Wayland desktop

## Intent and agreed scope

Run and visually inspect `dawn4k-demo` from a Mac with Docker Desktop, without a
physical Linux machine. The user and the assistant must be able to view the same
desktop in a browser. The user approved retaining Compose/AWT and using XWayland:
native Wayland presentation is not required.

Success means a visible particle scene with functioning Compose controls,
resizing, and clean shutdown. GPU performance is not an acceptance criterion.
macOS/Metal and Windows/D3D12 behavior must remain intact.

## Current project constraints

- `Main.kt` currently accepts only macOS and Windows.
- `SurfaceHost` separates native window lifetime from Dawn presentation.
- Windows uses a dedicated child window beside Compose controls, avoiding
  concurrent rendering into Compose's own native target.
- Dawn already supports Vulkan; generated bindings include
  `WGPUSurfaceSourceXlibWindow`.
- JVM native resources currently stage Linux x64 Dawn, and the demo declares
  Linux x64 Skiko. Extend both to Linux ARM64 while retaining Linux x64 support.
- The pinned dawn-packer release `v8077.0.0` publishes both shared and static
  `linuxArm64` archives. This was verified against the release asset list;
  their absence from the project's lock is not an upstream limitation.
- The demo requires JDK 25 and native access for Java FFM.
- Docker Desktop on this machine runs Linux ARM64. The development container
  will explicitly target `linux/arm64`, without x64 emulation.
- Dawn prebuilts require a sufficiently recent glibc/libstdc++. Use Ubuntu 24.04
  as the initial base and verify the actual shared-library requirements during
  implementation; loading the pinned Dawn library is an acceptance gate.

## Architecture

```text
Mac browser / OpenChamber browser
              |
       localhost:6080 (HTTP + WebSocket)
              |
       noVNC + websockify
              |
       wayvnc (internal VNC port)
              |
       Sway headless desktop, software compositor
              |
       XWayland -> Compose/AWT + X11 particle child window
                                      |
                               Dawn Vulkan -> Mesa lavapipe
```

### Desktop container

Add the Dockerfile, Compose service, startup script, and Sway configuration under
`dawn4k-demo/docker/`. Provide a single documented Compose command from the repo
root. The image contains JDK 25, Sway, XWayland, wayvnc, noVNC, websockify,
Mesa's lavapipe Vulkan driver, diagnostic tools, a terminal, and required X11/AWT
libraries. Install JDK 25 from a versioned vendor distribution with checksum
verification.

Run desktop services as a non-root user with a private `XDG_RUNTIME_DIR`.
Use the wlroots headless backend and software rendering, without GPU devices,
host display mounts, or privileged mode. Enable software capture as needed by
the packaged Sway/wayvnc versions and verify that capture works with the chosen
headless configuration.

Expose only the browser endpoint, bound to `127.0.0.1:6080`. VNC stays inside the
container and does not require authentication in this local-only development
setup. This is not a remotely deployed service and must not be bound publicly.

Mount the checkout at `/workspace`. Use Docker volumes for Gradle caches and
Linux build output directories so Linux does not overwrite host build outputs.
Keep these mounts narrowly scoped; do not hide source or generated versioned
bindings. Allow writes to the mounted source tree and document this property.

The desktop starts independently of the demo, with a terminal and a documented
launcher. The launcher runs the demo through the Gradle wrapper, limits Dawn
downloads to Linux where possible, sets the software Vulkan ICD, and retains
logs. It can be invoked through `docker compose exec` or inside the terminal.
No demo build is required merely to view the desktop.

The startup script waits for compositor readiness, obtains the XWayland display
from Sway rather than guessing `DISPLAY`, and propagates it to launchers.
It supervises services, forwards shutdown signals, and fails clearly if a
required service exits. Readiness checks distinguish a reachable web endpoint
from a working captured desktop.

### Linux presentation host

Add `LinuxSurfaceHost` implementing `SurfaceHost`, backed by Xlib through Java
FFM, with `DawnBackend.Vulkan`. Follow the Windows design: own a separate native
child window in the scene viewport, not Compose's rendering target.

Obtain the Compose parent X11 window identifier through the existing supported
native-handle facility if available on Linux, validating its meaning before use.
Use a dedicated Xlib connection for the child; serialize all access to that
connection on one owner thread. Do not manipulate AWT's private display
connection or rely on inaccessible JVM internals. The X11 connection and child
window outlive the Dawn surface, and the parent identifier is borrowed.

Create/map the child, track the viewport bounds in physical pixels, resize or
hide it when needed, and process relevant X11 events without blocking Compose
or the render loop. Closing the parent must cancel rendering and release the
surface before destroying the child and closing its display connection.

Add `DawnSurface.createXlib` with the generated Xlib surface descriptor and
nonzero-handle validation. Query surface capabilities for the Linux path and
select a supported format, alpha mode, and present mode. Carry the selected
format into `ParticleScene` instead of assuming BGRA8 is universally supported.
Keep existing platform behavior unless a shared correction is required.

Generalize platform selection in `Main.kt` and `DemoApp.kt` so Linux and Windows
share the side-by-side controls/scene layout while macOS preserves its existing
layout and Metal host.

### ARM64 JVM packaging

Add the pinned Linux ARM64 archives to `bindings/dawn.lock.json` with verified
archive SHA-256 values from the release and stage the shared library under
`linux-aarch64`. Add the Linux ARM64 Skiko runtime to the version catalog and
demo dependencies. The desktop launcher requests `-Pdawn.targets=linuxArm64`.

The generated JVM loader already recognizes `aarch64`/`arm64`, but its embedded
bundle table currently contains only macOS ARM64 and Linux x64. Extend the
generated bootstrap through the existing binding-generation workflow so
`linux-aarch64` resources are extracted and their library-content checksums are
validated. Preserve existing bundle identities and inspect regeneration diffs;
do not replace the generated ABI bindings with unrelated changes. Verify the
ARM64 ABI against the pinned Dawn headers before claiming runtime support.
This scope adds JVM ARM64 support, not a new Kotlin/Native Linux ARM64 target.

## Error handling and observability

Desktop startup failures must identify the failing service in container logs.
The demo must report missing `DISPLAY`, unavailable Xlib, invalid parent handles,
missing Dawn dependencies, unavailable Vulkan adapters, unsupported surface
capabilities, and presentation failures clearly. Reuse the controls' existing
failure display and stdout frame logging.

Log the selected backend, adapter identity, surface configuration, and first
rendered frame. Configure the lavapipe ICD explicitly so software rendering is
reproducible. If Dawn rejects a software adapter, investigate the adapter
request options explicitly rather than silently changing rendering backends.

## Verification and acceptance

1. Unit tests cover Linux platform selection, Xlib descriptor validation,
   viewport sizing/lifetime logic, and format selection where testable without
   a desktop. Platform-native tests are gated appropriately.
2. Existing demo unit tests remain passing; Linux-specific code must not load
   Xlib during macOS or Windows execution.
3. Build the arm64 image on this Mac, verify its architecture and the ARM64 ABI,
   and load the pinned Dawn library through the JVM resource loader inside it.
4. Start Compose and confirm Sway, XWayland, wayvnc, and the browser endpoint
   are ready. Confirm no privileged mode or public port binding is used.
5. Open noVNC in OpenChamber and confirm a captured desktop, not just the viewer
   HTML. Record a screenshot as evidence.
6. Launch the demo, confirm Vulkan/lavapipe adapter selection and a rendered
   first frame in logs, and capture visible particles alongside the controls.
7. Test pause/resume, reset, count changes, resizing, and close. If browser tools
   cannot drive the noVNC canvas, use a narrowly scoped in-container input tool
   and confirm the visible result with screenshots; explain the manual user
   controls in the README.
8. Stop Compose and verify desktop/demo processes terminate cleanly. Volumes
   remain available for subsequent launches.

## Deliverables and non-goals

Deliver Linux demo support, local desktop-container configuration, automated
tests, and a README with launch/view/stop/rebuild commands, logs, and limitations.

Not included: native Wayland surfaces, rebuilding Dawn from source, GPU passthrough,
performance claims, remote hosting, CI desktop infrastructure, or unrelated
refactoring. If X11 child embedding cannot be made reliable, return to design
review rather than silently replacing the agreed integrated UI.

## Design workflow checklist

- [x] Explore repository and Docker constraints.
- [x] Clarify XWayland versus native Wayland requirements.
- [x] Compare alternatives and obtain scope/design approval in conversation.
- [x] Write design and review for ambiguity, contradictions, and scope.
- [x] Obtain user review of this written specification.
- [x] Write implementation plan after specification approval.
- [x] Obtain the user's choice of execution method before implementation (Native).
