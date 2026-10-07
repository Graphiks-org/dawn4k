# Native Wayland presentation for dawn4k-demo

## Intent and approved scope

Validate a real Dawn/Vulkan Wayland surface from the existing Linux ARM64 Docker
desktop, visible to the user and assistant through the same noVNC session.
The Wayland execution path must run without X11 or XWayland. Retain the existing
X11 demo and prove non-regression separately; do not replace it.

The user approved a native Wayland client with keyboard controls instead of the
Compose/AWT panel. Backend selection is automatic by default, not a required
command-line flag. GPU performance is not an acceptance criterion.

This extends the previous Linux desktop design: its XWayland architecture remains
the supported X11 path, but no longer defines all Linux executions.

## Backend selection

- Preserve existing macOS/Metal and Windows/D3D12 entry points.
- On Linux, a nonblank WAYLAND_DISPLAY or inherited WAYLAND_SOCKET selects Wayland.
  Otherwise a nonblank DISPLAY selects X11. Neither produces a clear diagnostic.
- Validate the selected connection during startup. An advertised but unusable
  Wayland connection is an error, even if DISPLAY exists: no silent X11 fallback.
- Provide an optional Linux-only override, `--platform=wayland` or `--platform=x11`,
  for reproducible tests and explicit user troubleshooting. No flag is required
  for normal launches. Reject invalid or unsupported overrides clearly.
- Select the path before creating UI objects or initializing Xlib. The Wayland
  path must not initialize Compose/AWT, Skiko's AWT runtime, or Xlib.
- Print the selected window-system backend and the Vulkan adapter in startup logs.

## Components and boundaries

### Native Wayland host

Add a dedicated Wayland SurfaceHost. It owns the Wayland connection and an
xdg-shell top-level window, exposes the live wl_display and wl_surface handles,
and implements pixel-size and lifetime contracts independently of AWT.

Use a small Linux-native C bridge with wayland-scanner-generated xdg-shell code
for protocol requests and listeners, called from Kotlin through Java FFM. This
keeps generated protocol marshalling and C callbacks out of hand-written JVM
bindings. It creates no Vulkan surface: Dawn alone owns Vulkan presentation.
Build and package the bridge for the existing Linux ARM64 and x64 JVM targets;
fail clearly on missing native artifacts or incompatible runtime libraries.

The host handles registry discovery, required globals, compositor ping/pong,
initial configure acknowledgement, subsequent configure events, keyboard focus,
top-level close, and connection errors. No frame may be presented before the
initial configure is acknowledged. Zero-sized compositor suggestions preserve a
valid chosen window size rather than configuring a zero-sized Dawn surface.

Keyboard input uses the compositor-provided keymap with libxkbcommon, respects
modifiers, and closes keymap file descriptors. Space toggles pause, R resets,
plus/minus select the adjacent available particle count without exceeding the
device limit, and Escape closes. Show instructions and current state in the
window title and logs. Closing is allowed before GPU initialization completes.

Maintain an explicit event-dispatch owner and synchronized command/state boundary
between it and the render worker. Do not hold host locks across blocking GPU
operations. Wake and stop event dispatch on shutdown; propagate terminal errors
to the application rather than leaving a live, frozen window.

Initially support integer output scale, updating buffer scale and physical pixel
dimensions coherently with surface configuration. Fractional-scale extensions
and client-side decorations are outside this change. Sway supplies resizing and
window-management gestures; the client does not depend on server decorations.

### Dawn surface

Add a Wayland factory in DawnSurface using the existing generated
WGPUSurfaceSourceWaylandSurface descriptor, with the correct chain type and live
wl_display/wl_surface pointers. Reuse capability negotiation, texture acquisition,
presentation, and borrowed-texture ownership rules.

The native host outlives the Dawn surface. Stop rendering and complete resource
cleanup before destroying the Wayland objects and closing their connection.
Do not attach independently rendered UI buffers to the same wl_surface.

### Shared renderer and entry points

Extract only GPU setup and the particle render loop from DemoApp into an
AWT-independent runner accepting SurfaceHost, ParticleControls, and cancellation.
Both Compose/X11 and native Wayland call that runner. Preserve the existing
platform capability-selection behavior and controls semantics.

The Wayland entry point owns the host and dispatch loop, and connects keyboard
requests to ParticleControls. Resize state is consumed by the render worker;
configuration and GPU calls retain the existing Dawn worker-thread ownership.
Close, setup failure, and compositor disconnection must cancel rendering and
release resources in order. Shutdown must not hang waiting for another event.

## Docker and browser validation

Keep the existing X11 desktop configuration and launcher usable for regression
tests. Add an independently selectable Wayland-only desktop configuration with
`xwayland disable`. Its supervisor, environment export, health check, and launcher
must require a usable Wayland endpoint, not DISPLAY.

The Wayland-only launch removes DISPLAY and inherited X11 configuration from the
demo environment. Verify that no XWayland process or X11 listener is active.
The image may retain X11 packages for the separate regression configuration;
their installation does not count as use by the Wayland execution path.

Retain Linux ARM64, JDK 25, lavapipe software Vulkan, Sway headless, wayvnc,
websockify/noVNC, non-root desktop services, and localhost-only publication.
Use separately named Compose projects/ports and cache volumes when running the
two desktops concurrently. Do not stop or replace the current desktop implicitly.

## Verification and acceptance

1. Unit tests cover automatic selection, override validation, unusable Wayland
   with DISPLAY present, controls, configure/resize state, and shutdown ordering.
2. A native layout/export oracle checks the C/FFM bridge ABI. Compile both Linux
   architectures; run the real desktop integration on ARM64 without emulation.
3. The distributed launcher and Gradle launch both start automatically in a
   Wayland-only environment without DISPLAY or XWayland. Logs identify Wayland
   and a real Dawn/Vulkan adapter. Missing endpoints produce useful errors.
4. Sway identifies the demo as a native Wayland client. Capture the actual window
   through noVNC and verify a visible, evolving particle scene and advancing
   presentation counters, not merely successful surface creation.
5. Exercise keyboard pause/resume, reset, count changes, native window resizing,
   integer scale change, Escape and compositor close. Verify the configured pixel
   size and continued presentation after resize. Test close during startup and
   compositor disconnection with bounded, clean termination.
6. Run the existing X11 native desktop suite in the X11 configuration with the
   explicit override; verify controls, resize, hiding/restoration and shutdown.
   Run JVM suites and Dawn ABI checks; keep opt-in live tests non-cacheable.
7. Document auto-detection, forcing a backend, browser access, keyboard controls,
   both desktop configurations, commands, dependencies, and troubleshooting.

macOS JVM tests are runnable on the current host. Windows and Linux x64 runtime
validation must be reported as unexecuted unless actually performed; compilation
or OS-gated JVM tests are not equivalent to native presentation validation.

## Non-goals

No Compose Wayland port, graphical control panel for this path, SDL dependency,
X11 removal, hardware GPU benchmarking, fractional scaling, touch input, or
general-purpose Wayland toolkit. No change to public Dawn API is required solely
to expose this demo host.
