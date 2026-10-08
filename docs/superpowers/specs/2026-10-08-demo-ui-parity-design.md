# Demo UI parity on native Wayland and existing desktop environments

## Intent and superseded decision

The user requests the same graphical demo in every environment and removal of
the application shortcuts introduced for Wayland. The former keyboard-only
Wayland acceptance decision is superseded. Linux Wayland must still operate
without X11 or XWayland; the existing X11, macOS and Windows backends remain.

The approved direction is to reuse the existing Compose control panel, varying
only native window/surface hosting. Runtime Wayland support is not evidence of
Compose/Skiko compatibility: integration must be demonstrated on Linux ARM64.

## User-visible contract

- One application window containing the same Compose controls and particle view.
- A shared content layout: the current non-macOS arrangement, control column on
  the left and particle viewport on the right. Use the same logical spacing,
  colors, labels and selected/disabled/error states on every platform. Native
  window borders and platform font rasterization may differ.
- Pause/Resume, Reset, Close and the same device-permitted particle-count buttons
  are visible and clickable. Device limits may change the available counts, not
  the control model or layout rules.
- All controls except Close are disabled until GPU initialization succeeds.
  Initialization and terminal errors appear in the common panel. Close remains
  usable during initialization and after failure.
- Space, R, +, minus and Escape no longer act as global application commands.
  Standard focused-button activation/focus traversal is not a custom shortcut
  and remains available through Compose. Compositor/window-manager shortcuts are
  outside application control.
- Remove shortcut hints from window titles, documentation and tests; no hidden
  keyboard-only fallback or separately implemented imitation of the panel.
- Preserve automatic Linux backend selection and optional explicit overrides.
  A failed Wayland launch must never quietly open an X11 UI.

## Current implementation and integration gap

`ParticleControlPanel.kt` already provides a reusable composable backed by
`ParticleControls`. `DemoApp.kt` hosts it beside a native Dawn viewport on X11
and Windows, while macOS has a transparent/overlay arrangement. `Main.kt`
dispatches Wayland into an independent C/FFM host with no Compose content.
`WaylandSurfaceHost.kt` currently translates protocol key events to control
actions and writes shortcut hints into the title. These differences must end.

The inspected JetBrains Runtime `jbr25` sources contain `WLComponentPeer`,
confirming AWT can have a native Wayland backend. The inspected upstream Skiko
Linux direct software renderer obtains an X11 drawing surface and calls
`XCreateGC`/`XPutImage`. That renderer cannot be assumed to run natively on
Wayland merely by selecting WLToolkit. Neither observation establishes the
behavior of the pinned Compose 1.11.1 / Skiko 0.144.6 artifacts on ARM64.

## Architecture and qualification gate

### Common content and model

Use one composable content definition for the panel and viewport layout, and
the existing ParticleControls/particle renderer. Native hosts receive viewport
bounds and input/lifecycle updates; they do not decide which buttons exist.
macOS changes from its overlay-specific composition to the shared layout, with
Metal constrained to the actual viewport rather than covering the controls.
No public Dawn binding API change is required solely for this demo.

### Preferred hosting: Compose with a verified native Wayland runtime

Qualify an upstream JetBrains Runtime build supporting JDK 25 and Linux ARM64,
with the project's actual Compose/Skiko artifacts. Keep dependency/runtime
versions explicit, authenticated and reproducible. Qualification must show:

1. Native toolkit selection before any UI initialization, DISPLAY absent and
   XWayland disabled, without implicit renderer fallback to an X11 host.
2. The actual Compose panel renders and accepts pointer clicks at scales 1 and 2.
3. A child Wayland rendering surface can occupy only the particle viewport and
   coexist with Compose without sharing conflicting buffer ownership.
4. Dawn/Vulkan renders particles on that surface while panel state updates,
   resizing and Close remain functional.
5. The hosting mechanism exposes stable enough native handles/lifetimes for a
   pinned implementation; an opaque windowHandle is never treated as wl_surface
   without checking its documented or inspected representation.

Do not change the desktop's default JVM or remove the working host until this
qualification passes. A failed test is a blocker to this hosting choice, not
permission to re-enable XWayland. No upstream fork is silently adopted.

### Approved alternative: common Compose content in the native Wayland host

If the preferred hosting fails qualification, retain the C/FFM Wayland window
and embed the same Compose content through a supported Compose scene API.
Render the UI with Skia raster resources to a Wayland UI surface; place Dawn's
Vulkan particle surface in a separate child/subsurface at the shared viewport
bounds. This is a backend adapter, not a second button implementation.

The implementation plan must include qualification of the scene API and its
ARM64 native library requirements. UI raster buffers and Vulkan buffers have
separate ownership; wl_buffer release controls UI buffer reuse. Pointer enter,
leave, motion and button events are forwarded to Compose in logical coordinates
with coherent event ordering, so Compose performs hit testing and clicks.
Keyboard focus events support normal control accessibility, without reinstalling
the removed global actions. Unsupported input/renderer paths fail explicitly.

This alternative must pass the same one-window, native-protocol, button and
lifecycle criteria. If neither hosting choice can satisfy them, stop and report
the concrete blocker; do not ship reduced controls as success. The qualification
result selects the adapter and is recorded before production integration.

## Ownership, rendering and errors

- A single application owner controls termination; button Close and native
  close both cancel rendering and release GPU resources before native surfaces.
- Preserve the terminal-error correction: late initialization cannot erase a
  Wayland connection failure. UI shows the first terminal cause while possible.
- A surface is presented by one renderer only. UI and particles must not both
  attach buffers to the same wl_surface. If shared native connections are used,
  retain private event queues and correct prepare-read/read/cancel pairing.
- UI lifecycle and event operations run on their owning dispatcher/thread.
  Simulation/GPU work stays off that dispatcher. Do not block UI indefinitely
  waiting for GPU setup or a frame.
- Logical viewport coordinates are converted to physical buffer dimensions
  consistently. Integer scale changes update UI hit testing, child position,
  buffer scale and Dawn configuration. No rendering beneath control hit targets.
- On compositor loss, release owned resources and exit with failure; on user
  Close, exit successfully. Retain bounded cleanup tests and their limitation:
  controlled operation checkpoints do not simulate an indefinitely hung driver.

## Validation and evidence

Tests exercise the shipped common composable, not a model-only imitation.
Use pointer interaction in the Wayland-only desktop, never xdotool or shortcut
activation to stand in for missing buttons. Verify each visible action and the
selected count, initialization/disabled/error states, resize, integer scaling,
window close, startup close and compositor disconnection.

Explicit negative tests prove unfocused-button R/+/-/Escape and Space do not
invoke global actions. Tests targeting a focused button distinguish its normal
activation behavior from a global shortcut.

Compare control labels, logical layout and visible states across Wayland and
X11; run the common-content tests on all available platforms. Validate macOS
native composition after replacing its overlay arrangement. Do not claim native
Windows evidence unless actually executed on Windows. Keep those limitations
in the report rather than equating OS-gated test counts with native coverage.

Preserve the existing viewer on 6080. Qualify in a separate disposable desktop,
then update the visible Wayland desktop on 6081 only after native integration
passes. Compositor-loss tests use their isolated 6082 project, never a visible
desktop. Full forced Dawn builds run serially because the existing `.dawn/extract`
scratch directory is shared across source mounts.

## Out of scope

Hardware-GPU benchmarks, fractional-scale expansion, public binding changes,
new control features and speculative upstream fork maintenance. Standard OS
window decorations need not be pixel-identical; demo content and behavior must.

## Source references and evidence status

- Local: ParticleControlPanel.kt, DemoApp.kt, Main.kt, WaylandSurfaceHost.kt,
  gradle/libs.versions.toml.
- https://blog.jetbrains.com/platform/2026/02/wayland-by-default-in-2026-1-eap/
- https://github.com/JetBrains/JetBrainsRuntime/blob/jbr25/src/java.desktop/unix/classes/sun/awt/wl/WLComponentPeer.java
- https://github.com/JetBrains/skiko/blob/master/skiko/src/awtMain/cpp/linux/SoftwareRenderer.cc
- https://github.com/JetBrains/skiko/blob/master/skiko/src/awtMain/kotlin/org/jetbrains/skiko/renderer/LinuxSoftwareRenderer.kt

Source inspection only at this design stage. No native Compose qualification
has yet been performed, and no existing runtime or application behavior has
been changed. The written implementation plan follows user review of this spec.
