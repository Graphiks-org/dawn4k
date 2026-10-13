# Dedicated Dawn acid-test module

## Intent and agreed scope

Run the published Graphiks WebGPU acid tests against Dawn without putting suite dependencies or execution code in the binding. The user approved a separate, unpublished Gradle module in this repository, named `:dawn4k-acid-tests`, targeting JVM, macOS ARM64, and Linux x64.

Consume `org.graphiks:suite-acid-tests` and its complete `foundationCases()` registry rather than copying upstream cases. This work does not introduce browser runners, demos, benchmarks, or iOS/Android targets.

## Module boundary

The new module depends on the public `:dawn4k` API and the published suite. Test execution and platform support belong to this module; it must not import binding internals or reuse the binding's private test sources. It is not a published library and adds no public binding API solely for testing.

Register the module in `settings.gradle.kts`. Use repository tooling versions and runtime/linking conventions, while declaring only the three desktop targets. JVM needs the existing Dawn native runtime configuration and native-access flag. Native test executables need the Dawn library and runtime search paths; Linux executables are linked and run only on Linux hosts.

## Execution contract

Discover every case through `foundationCases()`. Require unique case IDs and retain those IDs in diagnostics and results. Each case receives its own Dawn context, fresh adapter and device. Negotiate the case's declared required features when requesting that device. Missing declared optional features produce `unsupported`, not a pass or assertion failure.

Pass an `AcidContext` with the borrowed device and a real adapter factory forwarding adapter options. Each factory invocation requests a fresh adapter. The runner owns its context, initial adapter and device; upstream cases own additional resources they acquire. Close resources in reverse ownership order and drain Dawn events before closing the context.

Use the public event-progress facilities to drive asynchronous Dawn operations. Bound case execution with a timeout that uses real elapsed time rather than virtual coroutine-test time. Timeouts and GPU errors fail the run. Unexpected uncaptured errors are recorded and fail the associated case; expected validation errors remain managed by the upstream cases. The runner must not add broad error scopes that alter the cases' validation semantics.

Continue collecting independent case results after recoverable case failures, then fail the Gradle test invocation if any case failed. Native process crashes remain test-task failures; never synthesize successful results for cases that did not finish.

## Results and availability

Record one terminal status per registry entry: `passed`, `failed`, `unsupported`, or `not-run`, with the case ID and a reason where applicable. Produce a machine-readable report in the module's build directory, with a summary and the selected target/backend, suite version, and available adapter information. Standard Gradle test reports supplement, but do not replace, these per-case results.

Preserve `DAWN_TEST_BACKEND` selection and `DAWN_REQUIRE_ADAPTER` strict mode. Only an actual adapter-unavailable response permits `not-run`; other adapter errors and device-request failures are failures. In strict mode, unavailable adapters fail the invocation. Do not classify every request failure as adapter unavailability. Determine availability using the existing public request outcome without importing an internal exception or exposing a new binding API; if it cannot distinguish unavailable safely, fail the request rather than incorrectly skipping it.

GPU execution is external state: disable up-to-date and build-cache reuse for execution tasks. CI requires an adapter, so missing GPU coverage cannot produce a green acid-test run. Unsupported declared optional features remain visible and are not claimed as executed coverage.

## Binding cleanup

Remove the suite dependency from `dawn4k/build.gradle.kts` and the two published buffer-case calls in `PublicContractGpuTest`. Remove their now-empty wrapper test and update its documentation. Preserve all binding-specific contract tests and their private test support. Remove the desktop-only intermediate source set if no longer needed and place the remaining contract test with the binding's common tests, after checking it is portable across its existing targets.

## CI and documentation

The root JVM/all-tests invocations should discover the new module normally. Extend the explicit Linux CPU Vulkan invocation to run both new JVM and Linux Native acid-test tasks, with `DAWN_REQUIRE_ADAPTER=1`, and upload their standard and machine-readable reports even on failure. Document the dedicated module commands, supported targets, availability policy, and the meaning of all four statuses. No iOS suite execution is claimed.

## Verification and acceptance

- CPU tests cover registry uniqueness, aggregation, feature selection, timeout/failure classification, and report serialization without needing a GPU.
- A regression check ensures the binding no longer depends on the acid-suite artifact.
- Compile the new JVM and host Native targets and execute the entire published registry against an available local adapter in strict mode.
- Run affected binding contract tests to verify their preservation after removing the suite dependency.
- CI executes JVM and Linux Native with CPU Vulkan; report local and remote evidence separately and do not claim unexecuted target coverage.
- Complete registry accounting is required: every discovered ID has exactly one terminal result, and any failed case makes the invocation fail.

## Workflow checklist

- [x] Inspect repository and upstream consumption contract.
- [x] Confirm module location and design with the user.
- [x] Write specification and self-review scope, consistency, and ambiguity.
- [x] User reviews this written specification.
- [x] Write implementation plan after specification approval.
- [ ] User selects execution method for the implementation plan.
- [ ] Implement and verify.
