# Dawn particle demo

## Desktop launch

Use JDK 25 and run from the repository root:

```bash
./gradlew :dawn4k-demo:run
```

The demo uses Dawn/Metal on macOS, Dawn/D3D12 on Windows and Dawn/Vulkan
on Linux. The shared controls provide Pause/Resume, Reset, particle counts,
and Stop/Restart.

## Mobile launch

### Android

Install Android SDK 37 and NDK 28.2.13676358; set `ANDROID_HOME` to the SDK directory.
Build the app and install it on a running emulator or device:

```bash
./gradlew :dawn4k-demo-androidApp:assembleDebug
adb install -r dawn4k-demo/androidApp/build/outputs/apk/debug/androidApp-debug.apk
adb shell am start -n org.graphiks.dawn4k.demo.app/.MainActivity
```

The app uses a Dawn/Vulkan native surface and the shared Compose controls.

### iOS

On macOS with Xcode installed, open
`dawn4k-demo/iosApp/Dawn4kDemo.xcodeproj`, select the `Dawn4kDemo` scheme and an
ARM64 iOS simulator, then run. The build phase assembles the Kotlin framework.
Device builds require configuring code signing; simulator builds do not.

The app uses a Dawn/Metal layer and the shared Compose controls. The checked-in
Xcode project can be regenerated with XcodeGen from `iosApp/project.yml`.

## Linux display selection

A nonblank `WAYLAND_DISPLAY` or inherited `WAYLAND_SOCKET` selects native
Wayland. Otherwise `DISPLAY` selects X11. An unusable Wayland connection
is an error; there is no silent fallback to X11.

Optional overrides, inside a native Linux graphical session:

```bash
./gradlew :dawn4k-demo:run --args=--platform=wayland
./gradlew :dawn4k-demo:run --args=--platform=x11
```

Linux bridge builds require a C compiler, `pkg-config`, Wayland development
headers, `wayland-scanner`, the stable xdg-shell protocol XML and xkbcommon
development headers. Runtime requires Wayland/xkbcommon libraries, Vulkan
and an appropriate driver. The Gradle build packages the host bridge.

For Linux ARM64 to x64 cross-compilation, install `x86_64-linux-gnu-gcc`,
set `DAWN_WAYLAND_X64_SYSROOT` to a compatible Linux sysroot, and select
`-Pwayland.targets=linuxArm64,linuxX64`.

## Tests

```bash
./gradlew check
```

On Linux, run native bridge tests with `:dawn4k-demo:testWaylandBridge`.
Set `DAWN_DESKTOP_TESTS=1` for live X11 tests or `DAWN_WAYLAND_TESTS=1`
for live Wayland tests in the corresponding graphical session. Without
these opt-ins, live desktop tests do not create native windows.
