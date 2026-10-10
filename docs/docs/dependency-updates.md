# Dependency updates

How to move `:dawn4k` when the WebGPU contract snapshot or the Dawn native
release evolves. Two pinned input sides must stay coherent: the Kotlin WebGPU
contract (`webgpu-api` / `webgpu-descriptors`) and the generated Dawn bindings
(`:dawn4k-native`).

## Pinned inputs

| Input | Pins |
| --- | --- |
| `bindings/dawn.lock.json` | the Dawn release (`v8077.0.0`, tag `chromium/8077`, revision and per-archive SHA-256) served to both kextract and the prebuilt libraries |
| `bindings/kextract.version` | the kextract commit that generated `org.graphiks.dawn4k.native` |
| `bindings/callback-bindings.yml` | the validated callback contracts kextract emits |
| Gradle | `org.graphiks:webgpu-api:0.1.0-SNAPSHOT`, `org.graphiks:webgpu-descriptors:0.1.0-SNAPSHOT` (the contract), `org.graphiks:suite-acid-tests:0.1.0-SNAPSHOT` (test scope) |

The resolved snapshot is what the backend compiles against — the sources jars
in the Gradle cache, not a master clone.

## Coordinating a snapshot evolution

When the contract artifacts or the generated bindings move:

```bash
./gradlew --refresh-dependencies :dawn4k:compileKotlinJvm :dawn4k:jvmTest
./gradlew :dawn4k:gpuTestJvm
```

The first command re-resolves the snapshot artifacts and re-checks the pure
surface (mapper tables, dispatcher, pending-operation and registry logic). The
second runs the full GPU suites against a real adapter. Finish with the other
targets:

```bash
./gradlew :dawn4k:macosArm64Test :dawn4k:gpuTestMacosArm64
./gradlew :dawn4k:compileKotlinLinuxX64
```

(The Linux test binaries link only on a Linux host; `compileKotlinLinuxX64` is
the achievable Linux gate elsewhere — see `dawn4k/build.gradle.kts`.)

## When a symbol gap appears

A resolved snapshot can name a member the pinned Dawn release does not serve —
or the generated bindings can expose a native symbol the contract does not
model. **A symbol gap is an upstream increment, never a local stub:**

1. open the increment upstream — the WebGPU contract snapshot (webgpu-api /
   webgpu-descriptors) and/or the Dawn prebuild (`dawn.lock.json`,
   `kextract.version` through the dawn-packer pipeline);
2. regenerate the bindings and consume the new snapshot/release;
3. re-run the gates above.

No mandatory method of the resolved contract may be postponed as
"unsupported" to keep a report green: the backend implements every member the
snapshot defines, and a member that cannot be served is a coordination bug to
fix upstream, not a local decision. The accepted-and-ignored fields (documented
in the mappers, e.g. the spec-deprecated `xrCompatible` hint or the
compilation hints with no Dawn counterpart) are exactly that — documented
consumption decisions, not postponed methods.
