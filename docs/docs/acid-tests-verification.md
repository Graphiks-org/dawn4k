# Acid-suite validation

The unpublished `:dawn4k-acid-tests` module checks the public Dawn binding against
the [published Graphiks WebGPU acid suite](https://github.com/Graphiks-org/WebGPU).
It executes the complete `foundationCases()` registry without copying cases,
filtering failures or weakening assertions. The binding itself does not depend
on the suite.

## Scope and execution

The module contains only the published-registry test, not infrastructure tests or
synthetic GPU fixtures. It runs on JVM and on Kotlin/Native for macOS ARM64 and
Linux x64. Windows runs the JVM test with D3D12. There are no Android, iOS, tvOS
or browser acid-suite targets.

Each case receives a fresh context, adapter and device. A dedicated thread keeps
Dawn's error scopes on the same thread across coroutine suspensions. Declared
optional features are requested explicitly. Case execution has a 30-second
deadline; teardown draining is bounded to 10 seconds.

Run commands from the repository root:

```bash
# macOS ARM64
DAWN_REQUIRE_ADAPTER=1 ./gradlew :dawn4k-acid-tests:jvmTest :dawn4k-acid-tests:macosArm64Test --continue

# Linux x64, with Vulkan configured
DAWN_TEST_BACKEND=Vulkan DAWN_REQUIRE_ADAPTER=1 ./gradlew :dawn4k-acid-tests:jvmTest :dawn4k-acid-tests:linuxX64Test --continue
```

```powershell
# Windows JVM
$env:DAWN_TEST_BACKEND = "D3D12"
$env:DAWN_REQUIRE_ADAPTER = "1"
.\gradlew.bat :dawn4k-acid-tests:jvmTest --continue
```

Root `jvmTest`, `allTests` and `check` also include the module. GPU execution is
not cached. Compilation alone does not demonstrate GPU execution; native tests
must run on a compatible host.

## Adapter configuration

`DAWN_TEST_BACKEND` selects `Metal`, `Vulkan` or `D3D12`. If unset, Dawn chooses
the backend. `DAWN_REQUIRE_ADAPTER=1` requires adapter availability.

On Linux, install `libvulkan1`, `mesa-vulkan-drivers` and `vulkan-tools`. Select
Mesa lavapipe by setting `VK_DRIVER_FILES` to the installed `lvp_icd*.json`, then
check the driver with `vulkaninfo --summary`. A CPU Vulkan adapter can exercise
buffer, compute and offscreen rendering tests; it does not replace Metal or
window-system coverage. Root tests also build the demo's Wayland bridge, which
requires `build-essential`, `pkg-config`, `wayland-protocols`, `libwayland-dev`
and `libxkbcommon-dev`.

On Windows, the Gradle runtime convention downloads the verified Dawn archive
and configures the external DLL search path for JVM tasks.

The acid runner uses only public APIs, which do not safely distinguish adapter
unavailability from other adapter-request failures. Consequently, **every adapter
request failure fails the acid execution**, even without strict mode. Error text
is never used to infer unavailability.

The binding's own GPU tests have a different policy: only Dawn's typed
`Unavailable` status permits non-execution outside strict mode, accompanied by
`WARNING: GPU_TEST_NOT_EXECUTED`. JVM uses a JUnit assumption. Kotlin/Native
returns before the test body and its standard report may count the invocation as
passed. That warning means **no GPU coverage**, not a successful assertion.
Adapter errors, device failures, assertion failures and native crashes remain
failures.

## Reading the results

Per-case JSON reports are written to
`dawn4k-acid-tests/build/reports/acid/<task-name>.json`. Standard Gradle reports
are under `dawn4k-acid-tests/build/reports/tests/`. JSON reports record the suite
source identity, target, selected backend, available adapter descriptions,
case diagnostics and status counts.

| Status | Meaning |
| --- | --- |
| `passed` | The published case completed successfully. |
| `failed` | A case assertion, execution or cleanup failed. |
| `unsupported` | A declared optional feature is absent, or the Compatibility case observes the permitted Core fallback. |
| `not-run` | Reserved for safely identified adapter unavailability; public adapter-request failures are classified as failures instead. |

Unsupported and not-run cases never count as passes. Strict mode rejects not-run
results. `complete=true` means every registry entry has a result, **not** that
every case passed. An incremental `complete=false` report is not successful
execution evidence. Previous reports are removed before each test task.

A failed case fails the local Gradle task after the remaining independent cases have
been collected. If guarded context closure fails, execution stops and the report
remains incomplete rather than reusing an unsettled environment.

## Contracts covered by mapping and validation cases

The per-case diagnostics identify the public contract that was not satisfied.
In particular:

- `buffers.map-cancel-remap` checks that cancelling mapping permits remapping.
- `buffers.map-destroyed` checks validation when mapping a destroyed buffer.
- `buffers.partial-map-remap` checks absolute buffer offsets for mapped ranges.
- `errors.overlapping-mapped-ranges` checks rejection of overlapping mapped ranges.
- `errors.unenabled-timestamp-feature` checks refusal of a timestamp operation
  without the required feature, including the expected error-delivery mechanism.

Use generated reports to determine which cases actually pass or fail on the
tested adapter. Failures are neither excluded nor converted into expected
successes.

## CI and limitations

The ordinary CI matrix runs JVM tests on macOS/Metal, Linux/CPU Vulkan and
Windows/D3D12. Deep validation uses root `check`, adding host-supported native
tests and Android/lint checks; Fast-Track uses root `jvmTest`. Apple simulators
are booted only on macOS. All runners require adapters and upload reports even
on failure. The required `build-and-test` check succeeds only if all runners pass.

CI passes `-Pdawn.acid.ignoreFailures=true`: published-case failures are advisory
while binding corrections are deferred. Cases still execute unchanged and their
failures remain visible in JSON and Gradle reports. Missing or incomplete acid
reports remain blocking, as do compilation, linking and all other module failures.
Local commands remain strict unless the same property is explicitly supplied.

Incremental report replacement is not atomic: a process or write failure can
lose the previous checkpoint. Coroutine deadlines are cooperative and cannot
interrupt a permanently blocked native call. A report missing completed results
must not be interpreted as a successful execution.
