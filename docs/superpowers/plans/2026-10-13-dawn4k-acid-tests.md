# Dedicated Dawn Acid Tests Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Execute the entire published Graphiks acid registry in an unpublished desktop module, removing acid dependencies from the binding.

**Architecture:** `:dawn4k-acid-tests` contains a portable runner and result model with platform-specific environment/report IO. Standard test tasks invoke it against the public Dawn API; each case has independent GPU resources and a real-time deadline. Binding-specific tests stay with the binding.

**Tech Stack:** Kotlin Multiplatform 2.4.20, Gradle 9.8.0, JVM/JDK 25, Kotlin/Native macOS ARM64 and Linux x64, kotlinx.coroutines, kotlinx.serialization, published Graphiks snapshots.

**Spec:** `docs/superpowers/specs/2026-10-13-dawn4k-acid-tests-design.md`

## Global Constraints

- Separate, unpublished module `:dawn4k-acid-tests` in this repository.
- Only JVM, macOS ARM64, and Linux x64 targets; no browser, Android or Apple mobile targets.
- Consume `org.graphiks:suite-acid-tests` and complete `foundationCases()`; do not copy upstream cases.
- No binding internals, private binding test sources, or new public binding APIs solely for tests.
- Per-case fresh context/adapter/device, real adapter factory, reverse-order cleanup and bounded event draining.
- Distinguish `passed`, `failed`, `unsupported`, and `not-run`; any failed case fails the invocation.
- Preserve `DAWN_TEST_BACKEND` and `DAWN_REQUIRE_ADAPTER`; ambiguous adapter failures fail rather than skip.
- Disable execution-task caching and up-to-date reuse; require adapters in CPU Vulkan CI.
- Preserve binding-specific tests and do not claim unexecuted target coverage.

## Review Focus

1. Device silently omits a requested feature: fail, never mark unsupported (Task 2).
2. External cancellation: propagate it, do not swallow it as a case failure or continue the suite (Task 2).
3. Errors during cleanup: retain the original failure and attach cleanup failures; never report pass (Task 2).
4. JSON diagnostics contain Unicode, quotes, newlines or backslashes: round-trip exactly (Task 1).
5. Empty or duplicate catalogue: fail before acquiring GPU resources (Task 1).

## File map

All module Kotlin paths below are relative to `dawn4k-acid-tests/src/`, with package `org.graphiks.dawn4k.acid`.

- `commonMain/kotlin/org/graphiks/dawn4k/acid/AcidReport.kt`: serializable results, catalogue validation and aggregate policy.
- `commonMain/kotlin/org/graphiks/dawn4k/acid/AcidSettings.kt`: strict environment parsing and backend configuration.
- `commonMain/kotlin/org/graphiks/dawn4k/acid/CaseExecution.kt`: feature validation and timeout/failure conversion.
- `commonMain/kotlin/org/graphiks/dawn4k/acid/DawnCaseRunner.kt`: resource ownership and event progression through public APIs.
- `commonMain/kotlin/org/graphiks/dawn4k/acid/Platform.kt`: environment and report-output expect declarations.
- `jvmMain/kotlin/org/graphiks/dawn4k/acid/Platform.jvm.kt`: JVM actuals.
- `nativeMain/kotlin/org/graphiks/dawn4k/acid/Platform.native.kt`: Native actuals using POSIX IO.
- `commonTest/kotlin/org/graphiks/dawn4k/acid/AcidReportTest.kt`, `AcidSettingsTest.kt`, `CaseExecutionTest.kt`: CPU policy tests.
- `commonTest/kotlin/org/graphiks/dawn4k/acid/PublishedAcidGpuTest.kt`: complete registry integration entry point.
- `dawn4k-acid-tests/build.gradle.kts`: unpublished desktop build and execution/report paths.
- Existing `settings.gradle.kts`, `dawn4k/build.gradle.kts`, `PublicContractGpuTest.kt`, `.github/workflows/ci.yml`, `README.md`: module registration, binding cleanup, CI and commands.

## Task 1: Create the module and CPU-tested result contract

**Interfaces:** `CaseResult(id: String, status: String, diagnostic: String? = null, missingFeatures: List<String> = emptyList(), adapterDescription: String? = null)`; `AcidReport(target: String, backend: String, suiteVersion: String, suiteCommit: String, cases: List<CaseResult>)`; `validateIds(ids: List<String>)`; `AcidReport.requireSuccessful(requireAdapter: Boolean)`; `AcidSettings.parse(backend: String?, requireAdapter: String?)`.

- [ ] Add `include(":dawn4k-acid-tests")` in `settings.gradle.kts`. Create the build with plain `kotlin("multiplatform")`, `alias(libs.plugins.kotlin.serialization)`, and `id("ygdrasil.conventions.dawn-jvm-runtime")`; do not use the publishing/Android library convention. Core configuration:

```kotlin
kotlin {
    jvmToolchain(25)
    jvm()
    macosArm64()
    linuxX64()
    applyDefaultHierarchyTemplate()
    sourceSets {
        commonMain.dependencies {
            implementation(project(":dawn4k"))
            implementation(libs.suite.acid.tests)
            implementation(libs.webgpu.descriptors)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.serialization.json)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test)
        }
        jvmTest.dependencies { implementation(kotlin("test-junit")) }
    }
}
```

- [ ] Write CPU tests before the policy implementations. Use `@Test`, Kotlin test assertions and `Json`:

```kotlin
@Test fun rejectsEmptyAndDuplicateRegistries() {
    assertFailsWith<IllegalArgumentException> { validateIds(emptyList()) }
    assertFailsWith<IllegalArgumentException> { validateIds(listOf("a", "a")) }
    validateIds(listOf("a", "b"))
}
@Test fun diagnosticRoundTrips() {
    val value = CaseResult("buffer.test", "failed", "λ \"quoted\"\n\\path")
    assertEquals(value, Json.decodeFromString<CaseResult>(Json.encodeToString(value)))
}
@Test fun failureAndStrictNotRunFail() {
    fun report(status: String) = AcidReport("jvm", "default", "v", "c", listOf(CaseResult("a", status)))
    assertFailsWith<IllegalStateException> { report("failed").requireSuccessful(false) }
    assertFailsWith<IllegalStateException> { report("not-run").requireSuccessful(true) }
    report("unsupported").requireSuccessful(true)
    report("not-run").requireSuccessful(false)
}
@Test fun parsesOnlySupportedEnvironmentValues() {
    assertEquals(false, AcidSettings.parse(null, null).requireAdapter)
    assertEquals(DawnBackend.Vulkan, AcidSettings.parse("Vulkan", "1").backend)
    assertFailsWith<IllegalArgumentException> { AcidSettings.parse(null, "yes") }
    assertFailsWith<IllegalArgumentException> { AcidSettings.parse("Typo", "0") }
}
```

- [ ] Run `./gradlew :dawn4k-acid-tests:jvmTest --tests '*AcidReportTest' --tests '*AcidSettingsTest'`; confirm failure on missing policy declarations rather than dependency-resolution failure. Fix scaffolding/resolution first if necessary; do not count that as a red policy test.
- [ ] Implement `@Serializable` result/report data classes with the exact fields above and the following policies:

```kotlin
fun validateIds(ids: List<String>) {
    require(ids.isNotEmpty()) { "Acid catalogue is empty" }
    require(ids.all { it.isNotBlank() }) { "Blank acid case ID" }
    require(ids.distinct().size == ids.size) { "Duplicate acid case IDs" }
}
fun AcidReport.requireSuccessful(requireAdapter: Boolean) {
    validateIds(cases.map { it.id })
    check(cases.all { it.status in setOf("passed", "failed", "unsupported", "not-run") })
    check(cases.none { it.status == "failed" || (requireAdapter && it.status == "not-run") }) {
        cases.filter { it.status == "failed" || (requireAdapter && it.status == "not-run") }
            .joinToString("\n") { "${it.id}: ${it.status}: ${it.diagnostic}" }
    }
}
data class AcidSettings(val backend: DawnBackend?, val requireAdapter: Boolean) {
    companion object {
        fun parse(backend: String?, requireAdapter: String?) = AcidSettings(
            backend?.let(DawnBackend::valueOf),
            when (requireAdapter) {
                null, "0" -> false
                "1" -> true
                else -> throw IllegalArgumentException("DAWN_REQUIRE_ADAPTER must be 0 or 1")
            },
        )
    }
}
```

- [ ] Repeat the targeted CPU tests; require green. Commit only the module registration, scaffolding, policies and tests.

## Task 2: Implement independent case execution and GPU ownership

**Interfaces:** consumes Task 1's model/settings; produces `suspend fun executeCase(id: String, timeout: Duration, body: suspend () -> CaseResult): CaseResult`, `fun checkDeviceFeatures(required: Set<GPUFeatureName>, actual: Set<GPUFeatureName>)`, and `suspend fun runDawnCase(case: AcidCase, settings: AcidSettings): CaseResult`.

- [ ] Read the resolved published sources, not just upstream master, for `AcidCase`, `AcidContext`, `SuiteBuildIdentity`, `foundationCases()` and `CompatibilityModeUnavailable`. Confirm `case.id.id`, the adapter factory signature, feature set types, and the compatibility fallback case ID. If the resolved artifact disagrees, update the plan's concrete API usages before implementation; do not copy cases to work around version mismatch.
- [ ] Write failing policy tests in `CaseExecutionTest.kt`:

```kotlin
@Test fun silentlyDroppedFeatureFails() {
    val feature = GPUFeatureName.entries.first()
    assertFailsWith<IllegalStateException> { checkDeviceFeatures(setOf(feature), emptySet()) }
}
@Test fun timeoutIsAFailure() = runBlocking {
    val result = executeCase("slow", 20.milliseconds) { delay(200); CaseResult("slow", "passed") }
    assertEquals("failed", result.status)
}
@Test fun cancellationPropagates() = runBlocking {
    assertFailsWith<CancellationException> {
        executeCase("cancelled", 1.seconds) { throw CancellationException("external") }
    }
}
@Test fun exceptionsKeepTheCaseId() = runBlocking {
    val result = executeCase("broken", 1.seconds) { error("oracle failure") }
    assertEquals("broken", result.id)
    assertEquals("failed", result.status)
    assertTrue(result.diagnostic!!.contains("oracle failure"))
}
```

- [ ] Run `./gradlew :dawn4k-acid-tests:jvmTest --tests '*CaseExecutionTest'` and observe the missing-function failure. Implement timeout conversion with real `Dispatchers.Default`, preserving external cancellation:

```kotlin
suspend fun executeCase(id: String, timeout: Duration, body: suspend () -> CaseResult): CaseResult =
    withContext(Dispatchers.Default) {
        try { withTimeout(timeout) { body() } }
        catch (failure: TimeoutCancellationException) { CaseResult(id, "failed", "Timed out after $timeout") }
        catch (cancelled: CancellationException) { throw cancelled }
        catch (failure: Throwable) { CaseResult(id, "failed", failure.stackTraceToString()) }
    }
fun checkDeviceFeatures(required: Set<GPUFeatureName>, actual: Set<GPUFeatureName>) {
    check(actual.containsAll(required)) { "Device dropped requested features: ${required - actual}" }
}
```

- [ ] Implement a consumer-owned context helper in `DawnCaseRunner.kt`. Use one coroutine dispatcher thread for event progression and case operations; `Dispatchers.Default.limitedParallelism(1)` plus `DawnConfig(settings.backend, implicitDeviceSynchronization = true)` avoids concurrent `processEvents()`. Do not import `ExplicitProgress.kt`. Use this control flow:

```kotlin
suspend fun <T> withDawnContext(settings: AcidSettings, body: suspend (DawnContext) -> T): T =
    withContext(Dispatchers.Default.limitedParallelism(1)) {
        coroutineScope {
            val context = DawnContext.create(DawnConfig(settings.backend, implicitDeviceSynchronization = true))
            val events = launch { while (isActive) { context.processEvents(); delay(1) } }
            var original: Throwable? = null
            try { body(context) }
            catch (failure: Throwable) { original = failure; throw failure }
            finally {
                try {
                    withContext(NonCancellable) { events.cancelAndJoin() }
                    val started = TimeSource.Monotonic.markNow()
                    while (context.hasPendingOperations()) {
                        check(started.elapsedNow() < 10.seconds) { "Dawn teardown did not settle" }
                        context.processEvents()
                    }
                    context.close()
                } catch (cleanup: Throwable) {
                    if (original != null) original.addSuppressed(cleanup) else throw cleanup
                }
            }
        }
    }
```

- [ ] Add a CPU-tested cleanup helper `fun preserveFailure(original: Throwable?, cleanup: Throwable)` implementing the catch block above, and call it from the helper. Test that an original `IllegalStateException("oracle")` keeps a cleanup exception in `suppressedExceptions`, and that cleanup with `original == null` throws. Use `use` for adapter/device acquisition; their existing close semantics preserve body exceptions.
- [ ] Implement `runDawnCase` with upstream cases untouched:

```kotlin
suspend fun runDawnCase(case: AcidCase, settings: AcidSettings): CaseResult =
    executeCase(case.id.id, 30.seconds) {
        withDawnContext(settings) { context ->
            context.requestAdapter().getOrThrow().use { adapter ->
                val missing = (case.requiredFeatures - adapter.features).map { it.name }.sorted()
                if (missing.isNotEmpty()) {
                    CaseResult(case.id.id, "unsupported", "Missing optional features", missing)
                } else {
                    val errors = mutableListOf<String>()
                    adapter.requestDevice(DeviceDescriptor(
                        requiredFeatures = case.requiredFeatures.toList(),
                        onUncapturedError = GPUUncapturedErrorCallback { errors.add(it.message) },
                    )).getOrThrow().use { device ->
                        checkDeviceFeatures(case.requiredFeatures, device.features)
                        case.run(AcidContext(device, requestAdapter = { options -> context.requestAdapter(options) }))
                        device.queue.onSubmittedWorkDone().getOrThrow()
                        context.processEvents()
                        check(errors.isEmpty()) { errors.joinToString("\n") }
                        CaseResult(case.id.id, "passed", adapterDescription = adapter.info.description)
                    }
                }
            }
        }
    }
```

Do not add blanket validation scopes. Any request failure currently becomes `failed`: the public API has no typed adapter-unavailable classification. Document this conservative behavior; do not parse exception text. `not-run` is reserved for safely identified unavailability if a public typed outcome exists in the resolved API. A `CompatibilityModeUnavailable` may become `unsupported` only for the specific declared compatibility case, matching upstream's verified policy; rethrow it for other case IDs. Add a CPU test for that ID guard before enabling the exception conversion.

- [ ] Configure Native compilation/linking in the module build, following `dawn4k/build.gradle.kts:72-127`: a `dawn` cinterop per desktop Native main compilation using `dawn4k-native/src/nativeInterop/cinterop/dawn.def`, staged `native/$targetName/shared/include`, staged library `native/$targetName/shared/lib`, `-lwebgpu_dawn`, and rpath. macOS uses Metal/Foundation/CoreGraphics/QuartzCore/IOKit/IOSurface; Linux uses pthread/dl/m. Add `prepareDawn` dependencies for cinterop and test-link tasks. Disable Linux test linking/execution off Linux. Do not create extra Native frameworks or publishing tasks.
- [ ] Run CPU tests and `./gradlew :dawn4k-acid-tests:compileKotlinMacosArm64 :dawn4k-acid-tests:compileKotlinLinuxX64`. Fix compiler/API disagreements without changing suite assertions. Commit runner, policy tests and Native configuration.

## Task 3: Wire complete registry execution, reports and binding separation

**Interfaces:** consumes Tasks 1-2; produces `expect fun environment(name: String): String?`, `expect fun targetName(): String`, `expect fun writeReport(json: String)`, and one complete-registry GPU test.

- [ ] Add expect/actual platform functions. JVM reads `System.getenv`, returns `"jvm"`, and writes through `java.io.File`. Native reads `platform.posix.getenv()?.toKString()`, returns `"macosArm64"` or `"linuxX64"` using `Platform.osFamily`, and writes UTF-8 via `fopen(path, "wb")`, `fwrite` on pinned bytes and `fclose`, checking write length and close return code. Gradle creates the parent directory before test execution. Both require `DAWN_ACID_REPORT_PATH`; neither silently writes to a guessed working directory.

```kotlin
actual fun writeReport(json: String) {
    val path = requireNotNull(environment("DAWN_ACID_REPORT_PATH")) { "Missing report output path" }
    java.io.File(path).writeText(json, Charsets.UTF_8)
}
```

Native actuals use `kotlinx.cinterop.*` and `platform.posix.*` with `@OptIn(ExperimentalForeignApi::class)`:

```kotlin
actual fun environment(name: String): String? = getenv(name)?.toKString()
actual fun targetName(): String = when (kotlin.native.Platform.osFamily) {
    kotlin.native.OsFamily.MACOSX -> "macosArm64"
    kotlin.native.OsFamily.LINUX -> "linuxX64"
    else -> error("Unsupported acid-test target")
}
actual fun writeReport(json: String) {
    val path = requireNotNull(environment("DAWN_ACID_REPORT_PATH"))
    val file = checkNotNull(fopen(path, "wb")) { "Cannot open report: $path" }
    var original: Throwable? = null
    try {
        val bytes = json.encodeToByteArray()
        if (bytes.isNotEmpty()) bytes.usePinned {
            check(fwrite(it.addressOf(0), 1.convert(), bytes.size.convert(), file).toLong() == bytes.size.toLong()) {
                "Incomplete report write: $path"
            }
        }
    } catch (failure: Throwable) { original = failure; throw failure }
    finally {
        if (fclose(file) != 0) preserveFailure(original, IllegalStateException("Cannot close report: $path"))
    }
}
```

Test platform IO on JVM with a task-provided report path; round-trip the file through `Json.decodeFromString<AcidReport>` after writing a fixture. The integration test exercises the same IO on Native.

- [ ] Add the GPU entry point in `PublishedAcidGpuTest.kt`:

```kotlin
@Test fun publishedFoundationRegistry() = runBlocking {
    val cases = foundationCases()
    validateIds(cases.map { it.id.id })
    val settings = AcidSettings.parse(environment("DAWN_TEST_BACKEND"), environment("DAWN_REQUIRE_ADAPTER"))
    val results = cases.map { case ->
        println("ACID_CASE ${case.id.id}")
        runDawnCase(case, settings).also { println("ACID_RESULT ${it.id}: ${it.status}") }
    }
    val report = AcidReport(targetName(), settings.backend?.name ?: "default",
        SuiteBuildIdentity.VERSION, SuiteBuildIdentity.COMMIT, results)
    check(results.map { it.id } == cases.map { it.id.id }) { "Incomplete registry accounting" }
    writeReport(Json { prettyPrint = true }.encodeToString(report))
    report.requireSuccessful(settings.requireAdapter)
}
```

Before each run, delete only its own previous report file from Gradle so a crash cannot leave stale evidence. Write incremental reports after each completed case with an explicit `complete: Boolean` field on `AcidReport` (default `true` for CPU fixtures, `false` until the final result). Update aggregation to reject incomplete reports and add a failing CPU test proving an incomplete report cannot pass. On abrupt process termination, keep only actual completed results with `complete=false`; do not fabricate statuses for missing entries.

Add `summary: Map<String, Int>` to the serialized report, defaulting to `listOf("passed", "failed", "unsupported", "not-run").associateWith { status -> cases.count { it.status == status } }`. Add a CPU assertion that the four counts sum to `cases.size` and that unsupported cases do not increase the passed count.

- [ ] Configure test execution tasks: native-access JVM flag, stdout, backend/strict/driver variables as inputs, `outputs.upToDateWhen { false }`, `outputs.doNotCacheIf(...) { true }`. Set `DAWN_ACID_REPORT_PATH` to an absolute per-task `build/reports/acid/<task-name>.json`; create its parent and remove its previous file in `doFirst`. For native executable tasks use their environment API as in existing Native test configuration. One report path per task prevents JVM/Native races.
- [ ] Run `DAWN_REQUIRE_ADAPTER=1 ./gradlew :dawn4k-acid-tests:jvmTest --tests '*PublishedAcidGpuTest'`. Require exact registry accounting and investigate any case failure as a real defect or integration issue, not by excluding the case. If unavailable locally, report the failure without claiming GPU coverage.
- [ ] Remove the acid dependency, `desktopTest` creation and its dependent source-set wiring from `dawn4k/build.gradle.kts`. Delete only `publicBuffersMatchTheSharedContract()` from `PublicContractGpuTest.kt`; revise its KDoc to describe binding-specific companion tests. Move the remaining file from `desktopTest` to `commonTest` with its package unchanged. Its imports use the same common helper/API as existing common GPU tests.
- [ ] Keep suite-specific verification out of the binding build; the dedicated external module alone owns integration of the published cases.

- [ ] Run `DAWN_REQUIRE_ADAPTER=1 ./gradlew :dawn4k:jvmTest --tests '*PublicContractGpuTest' :dawn4k-acid-tests:jvmTest :dawn4k-acid-tests:macosArm64Test` on the macOS host; compile affected mobile common tests with `:dawn4k:compileTestKotlinIosSimulatorArm64` to detect portability regressions. Record actual results, including skipped/unsupported cases, before committing.

## Task 4: Integrate CI, document commands and verify end to end

**Interfaces:** no new code interfaces; exposes module tasks and uploaded reports to users.

- [ ] Update `.github/workflows/ci.yml` CPU Vulkan step and upload paths:

```yaml
          ./gradlew :dawn4k:jvmTest :dawn4k:linuxX64Test :dawn4k-acid-tests:jvmTest :dawn4k-acid-tests:linuxX64Test --no-daemon --continue --stacktrace
```

Add `dawn4k-acid-tests/build/reports/` and `dawn4k-acid-tests/build/test-results/` to the existing always-upload artifact. Preserve `DAWN_TEST_BACKEND=Vulkan` and `DAWN_REQUIRE_ADAPTER=1`. Root JVM/allTests selectors already include the new module; confirm with dry-run rather than duplicating execution in the macOS workflow.

- [ ] Add a README architecture bullet and a dedicated acid-suite section containing:

```bash
DAWN_REQUIRE_ADAPTER=1 ./gradlew :dawn4k-acid-tests:jvmTest :dawn4k-acid-tests:macosArm64Test
DAWN_TEST_BACKEND=Vulkan DAWN_REQUIRE_ADAPTER=1 ./gradlew :dawn4k-acid-tests:jvmTest :dawn4k-acid-tests:linuxX64Test
```

Explain unpublished module boundaries, supported targets, `build/reports/acid/<task-name>.json`, all four statuses, `complete=false`, declared optional-feature handling, and conservative failure when adapter unavailability cannot be typed through the public API. Explicitly separate these semantics from the binding's private skip helper.

- [ ] Verify scheduling with `./gradlew jvmTest --dry-run` and `./gradlew allTests --dry-run`; ensure the new module tasks appear. Run `git diff --check`, the Task 3 strict host commands, and inspect generated JSON for unique IDs, completed accounting and statuses. Run Linux commands only on a Linux host; otherwise mark Linux execution pending CI, not passed.
- [ ] Compare `foundationCases()` IDs with the report and print pass/fail/unsupported/not-run counts. No successful final claim is allowed if the report is incomplete, any case failed, or adapter-unavailability was hidden.
- [ ] Commit CI/docs after verification. Request a whole-branch review according to the selected execution method, then report results and remaining platform limitations. Do not open a PR, push or merge without user authorization.

## Plan self-review

- Spec requirements map to Tasks 1 (module/model), 2 (isolation/features/deadlines/errors), 3 (registry/reports/separation), and 4 (CI/documentation/evidence).
- The five Review Focus conditions have explicit CPU tests in Tasks 1-2; incomplete-report rejection is added in Task 3.
- The runner must use resolved artifact signatures and public Dawn operations; adapter ambiguity fails conservatively as permitted by the spec.
- No execution has started. User review and execution-method selection are required next.
