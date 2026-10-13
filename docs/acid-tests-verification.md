# Published acid-suite verification

## Initial local evidence — 2026-10-13

The dedicated `:dawn4k-acid-tests` module consumes the published suite unchanged.
Resolved suite version: `0.1.0-SNAPSHOT`; suite source identity:
`f3a0ee3caa0ea3b09f0b91514c98d2dd3edf9fa9`.

Strict local execution against Dawn's default Metal adapter produced:

| Target | Registry entries | Passed | Failed | Unsupported | Not-run |
| --- | ---: | ---: | ---: | ---: | ---: |
| JVM | 154 | 149 | 5 | 0 | 0 |
| macOS ARM64 Native | 154 | 149 | 5 | 0 | 0 |

Both reports are complete; both Gradle invocations **fail**, as required when a
case fails. These are local results, not evidence of remote CI execution or Linux
GPU coverage. Linux x64 compilation was verified on the macOS host; Linux linking
and GPU execution require a Linux host and remain pending CPU Vulkan CI.

## Retained contract failures

- `buffers.map-cancel-remap`: cancelling a started mapping leaves it mapped/pending
  rather than immediately unmapped.
- `buffers.map-destroyed`: mapping a closed/destroyed buffer does not produce the
  validation error expected by the suite's error scope.
- `buffers.partial-map-remap`: mapped-range offsets are treated as relative to the
  mapped region instead of absolute buffer offsets.
- `errors.overlapping-mapped-ranges`: overlapping CPU-visible ranges are accepted.
- `errors.unenabled-timestamp-feature`: timestamp feature refusal produces an
  error-scope event where the public contract expects only a synchronous exception.

No case is filtered out and no published oracle was modified. Fixing these binding
semantics is a separate change; the module intentionally exposes them to CI.

The runner's initial Native execution also exposed thread migration across error
scope push/pop operations. Pinning each context to a consumer-owned dedicated
thread eliminated the 60 associated `No error scopes to pop` failures without
modifying the binding.

## Other checks

- Binding-specific `PublicContractGpuTest`: 11 JVM tests passed after relocation
  to `commonTest` and removal of its two published buffer-case calls.
- `:dawn4k:verifyNoAcidSuiteDependency`: passed; it checks resolved JVM test-runtime
  components rather than source text alone.
- `:dawn4k:compileTestKotlinIosSimulatorArm64`: passed, confirming the relocated
  binding tests compile for an existing mobile target; this is not iOS GPU evidence.
- Runner CPU policies and local GPU fixture tests pass independently of the five
  published contract failures. They exercise registry validation, JSON/UTF-8 IO,
  aggregate failure policy, real-time timeouts, cancellation, cleanup errors,
  optional-feature validation, adapter freshness and uncaptured errors.
- Full root JVM run (`DAWN_REQUIRE_ADAPTER=1 ./gradlew jvmTest
  :dawn4k:verifyNoAcidSuiteDependency --continue`): binding 223/223, native binding
  3/3 and demo 99/99 passed. The acid module has 23 passing runner/policy tests
  plus its complete-registry test, which fails on the five cases listed above.
- Root `jvmTest` and `allTests` task discovery was checked with `--dry-run`.

## Reproduce

```bash
DAWN_REQUIRE_ADAPTER=1 ./gradlew :dawn4k-acid-tests:jvmTest :dawn4k-acid-tests:macosArm64Test --continue
./gradlew :dawn4k:verifyNoAcidSuiteDependency
```

Inspect `dawn4k-acid-tests/build/reports/acid/jvmTest.json` and
`dawn4k-acid-tests/build/reports/acid/macosArm64Test.json`. Reports contain per-case
diagnostics and suite identity; standard Gradle test reports are under the same
module's `build/reports/tests/`. Later snapshots or driver versions may produce
different results; the table above records the identified initial run, not a
guarantee of a green future run.
