# dawn4k WebGPU Backend Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implémenter le contrat Graphiks WebGPU avec Dawn et vérifier des roundtrips buffer, compute et rendu offscreen avant d'exécuter le catalogue complet.

**Architecture:** Un runtime par contexte sérialise les appels et progresse l'instance Dawn. Des wrappers de ressources ciblés traduisent l'API publique sans copier ses interfaces. Les familles de ressources sont testées contre une fixture brute avant de raccorder Adapter/Device/Context publics, pour éviter des stubs de méthodes obligatoires entre incréments.

**Tech Stack:** dawn4k-native issu du plan 01, Kotlin 2.4.20, JDK 25, kffi, kotlinx.coroutines 1.11.0, Graphiks WebGPU 0.1.0-SNAPSHOT.

**Spec:** `docs/superpowers/specs/2026-10-02-dawn4k-design.md`.

## Global Constraints

- Lire le plan 01 et vérifier son gate avant le premier appel Dawn de ce plan.
- Publication `org.graphiks:dawn4k:0.1.0-SNAPSHOT` ; package `org.graphiks.dawn4k`.
- WebGPU api/descriptors `0.1.0-SNAPSHOT` évolutive, sans pin horodaté.
- Kotlin `2.4.20`, JDK `25`, Gradle `9.6.1`, macosArm64/linuxX64 et JVM desktop.
- Tous les noms/signatures GPU proviennent de la snapshot résolue ; baseline source examinée `2608e3f5202f8386bf391274083419808b99d857` seulement comme référence de recherche.
- Pas de stubs pour une méthode obligatoire, pas de faux Result.success, pas d'ordinal d'enum comme ABI.
- Tous les appels natifs utilisent les bindings générés ; aucun C++ direct, JNI manuel JVM ni fork des interfaces WebGPU.
- Les scopes FFI JVM restent sur leur thread, les callbacks copient immédiatement leurs données empruntées.
- Validation WebGPU invalide observée par les scopes natifs ; les erreurs host mémoire restent vérifiées avant accès.
- Queue/passes/bundle sans AutoCloseable dans la baseline ont un owner ; ne pas inventer des méthodes sur les interfaces externes.
- Aucune vue native n'est déréférencée après unmap/destroy pour prouver une erreur : c'est potentiellement de l'UB.
- Aucun test d'infrastructure à créer : conserver les tests du comportement WebGPU, de la mémoire et des callbacks ; la compilation et la revue d'inventaire suffisent pour contrôler la surface d'API, sans test/gate de coverage structurelle.

## Review Focus

1. Callback différée/inline et annulation concurrente : une livraison, résultat tardif relâché, tâches 1–2.
2. Close depuis callback/worker ou appel synchrone réentrant : pas de deadlock ni thread confiné déplacé, tâches 1–2.
3. Nulls, empty lists, ULong overflow et defaults non nuls : conversions explicites, tâches 3–6.
4. Handles étrangers et objets détruits : pas de mélange de devices ni UAF ; validation native conservée, tâches 2–3 et 6.
5. Erreurs de shader/pipeline/perte device : pas d'attente infinie ni diagnostic emprunté conservé, tâches 2, 4 et 7.

---

## Organisation des sources

Créer `dawn4k/build.gradle.kts` avec la convention desktop du plan 01,
`api(project(":dawn4k-native"))`, `api("org.graphiks:webgpu-api:0.1.0-SNAPSHOT")`,
`implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.11.0")`.
Les tests dépendent de `webgpu-descriptors`, `suite-acid-tests` et
`kotlinx-coroutines-test:1.11.0`. Si les classes publiques n'exposent aucun type
brut, réexaminer `api` vs `implementation` pour dawn4k-native avant publication.

Chemins Kotlin communs ci-dessous sous
`dawn4k/src/commonMain/kotlin/org/graphiks/dawn4k/` ; les chemins de tests sont
indiqués complètement. Un fichier par objet/descriptor : pas de mapper géant.

Les tests purs sont `commonTest` ; les tests GPU résident dans `commonTest` mais
ne sont exécutés que par tâches `gpuTestJvm`, `gpuTestMacosArm64` et
`gpuTestLinuxX64` créées avec leurs filtres, sans rendre les tests purs dépendants
d'un adapter. Exiger les deux ensembles dans le gate final.
Les tâches standard `jvmTest`, `macosArm64Test`, `linuxX64Test` excluent
explicitement les classes `*GpuTest` ; les tâches GPU incluent exclusivement
ces classes et échouent en absence de GPU. Réutiliser le même test binaire
K/N avec un filtre, ne pas inventer une tâche qui compile sans exécuter.

## Task 1: Dispatcher et terminalisation des opérations testés sans GPU

**Files:**
- Create: `dawn4k/build.gradle.kts` ; Modify: `settings.gradle.kts`.
- Create: `dawn4k/src/commonMain/kotlin/org/graphiks/dawn4k/internal/NativeDispatcher.kt`.
- Create: `dawn4k/src/commonMain/kotlin/org/graphiks/dawn4k/internal/PendingOperation.kt`.
- Create: `dawn4k/src/jvmMain/kotlin/org/graphiks/dawn4k/internal/NativeDispatcher.jvm.kt`.
- Create: `dawn4k/src/nativeMain/kotlin/org/graphiks/dawn4k/internal/NativeDispatcher.native.kt`.
- Test: `dawn4k/src/commonTest/kotlin/org/graphiks/dawn4k/internal/PendingOperationTest.kt`.
- Test: `dawn4k/src/commonTest/kotlin/org/graphiks/dawn4k/internal/NativeDispatcherTest.kt`.

**Interfaces:**
- `interface NativeDispatcher : AutoCloseable { fun <T> call(block: () -> T): T; suspend fun <T> awaitCall(block: () -> T): T; fun post(block: () -> Unit); suspend fun drain() }`.
- `fun createNativeDispatcher(): NativeDispatcher` expect/actual, owned worker et fast path réentrant.
- `class PendingOperation<T>(val releaseRejected: (T) -> Unit)` : `suspend fun await(): Result<T>`, `fun complete(result: Result<T>)`, `fun abandon(cause: Throwable)`, `fun nativeTerminal()` ; `val waiterFinished: Boolean`, `val nativeFinished: Boolean`.
- `abandon` termine l'attente ; `nativeTerminal` autorise le cleanup registration. Deux états distincts, pas un booléen unique pour tout.

- [ ] **Step 1 — test rouge de résultat tardif.**

```kotlin
@Test fun resultAfterCancellationIsReleasedExactlyOnce() = runTest {
    val released = mutableListOf<Int>()
    val operation = PendingOperation<Int> { released += it }
    val waiter = async { operation.await() }
    runCurrent()
    waiter.cancelAndJoin()
    operation.complete(Result.success(7))
    operation.nativeTerminal()
    assertEquals(listOf(7), released)
    assertTrue(operation.nativeFinished)
}
```

Ajouter tests : completion avant await, completion inline pendant enregistrement,
échec natif, annulation avant dispatch (aucun appel C), close pendant attente,
callback sur autre thread avec copie, réentrée `dispatcher.call { call { 42 } }`
et fermeture réentrante qui n'attend pas son propre worker. Simuler le scheduling,
pas la validation GPU. Une double livraison qui contient une nouvelle référence
possédée la relâche ; ne pas relâcher deux fois la même référence C inexistante.

- [ ] **Step 2 — Run** `./gradlew :dawn4k:jvmTest --tests '*PendingOperationTest' --tests '*NativeDispatcherTest'` ; échec attendu types absents.
- [ ] **Step 3 — implémenter la machine d'état et les workers.**

```text
waiter : waiting -> delivered | cancelled | owner-closed
native : not-started -> in-flight -> terminal
cancel before dispatch : not-started -> terminal, no C call
cancel after dispatch : waiter cancelled, native remains in-flight
native success + waiter unavailable : releaseRejected(result)
native terminal : cleanup copied result/registration after dispatch has returned
```

Toutes les transitions d'une operation sont sérialisées par le worker ; les
callbacks arrivant ailleurs postent un résultat possédé/copié. Employer
`suspendCancellableCoroutine`, `tryResume`/`completeResume` ou équivalent vérifié
de coroutines `1.11.0`, et le cleanup de valeur sur annulation après reprise.
Ne pas fermer une registration native simplement parce que la coroutine est
annulée. Worker JVM : thread unique avec dispatcher coroutines ; worker K/N :
dispatcher mono-thread supporté par la version résolue, jamais `Dispatchers.Main`
requis. Publier dans le type actual la fermeture différée réentrante.

- [ ] **Step 4 — vérifier le vert** tests JVM + tests purs natifs des deux hosts ; ajouter un stress de 1000 complétions/annulations avec compteur references=0 à la fin, sans sleep d'oracle.
- [ ] **Step 5 — commit** `git add dawn4k settings.gradle.kts` puis `git commit -m "feat(dawn4k): add serialized native operation lifecycle"`.

## Task 2: Runtime raw, sessions et propriétaires explicites

**Files:**
- Create: `dawn4k/src/commonMain/kotlin/org/graphiks/dawn4k/internal/DawnRuntime.kt`.
- Create: `dawn4k/src/commonMain/kotlin/org/graphiks/dawn4k/internal/DeviceSession.kt`.
- Create: `dawn4k/src/commonMain/kotlin/org/graphiks/dawn4k/internal/ResourceRegistry.kt`.
- Create: `dawn4k/src/commonMain/kotlin/org/graphiks/dawn4k/internal/NativeErrors.kt`.
- Create: `dawn4k/src/commonMain/kotlin/org/graphiks/dawn4k/DawnConfig.kt`.
- Create: `dawn4k/src/commonTest/kotlin/org/graphiks/dawn4k/testing/NativeFixture.kt`.
- Test: `dawn4k/src/commonTest/kotlin/org/graphiks/dawn4k/internal/ResourceRegistryTest.kt`.
- Test: `dawn4k/src/commonTest/kotlin/org/graphiks/dawn4k/internal/DawnRuntimeGpuTest.kt`.
- Modify: `dawn4k/build.gradle.kts` (tâches de tests GPU distinctes).

**Interfaces:**
- `class DawnRuntime(config: DawnConfig) : AutoCloseable` owns WGPUInstance + NativeDispatcher ; `suspend fun openSession(): DeviceSession`, `suspend fun drainEvents()` ; créé par Context en tâche 7.
- `class DeviceSession : AutoCloseable` owns raw adapter/device/queue ; propriétés internes `runtime: DawnRuntime`, `handle: WGPUDevice`, `queueHandle: WGPUQueue`, `resources: ResourceRegistry`.
- `class ResourceRegistry` : `fun own(key: Any, destroy: (() -> Unit)?, release: () -> Unit)`, `fun destroy(key: Any)`, `fun release(key: Any)`, `fun close()` ; appels sur dispatcher, pas threads arbitraires.
- `data class DawnConfig(val backend: DawnBackend? = null)`, `enum class DawnBackend { Metal, Vulkan }` dans `DawnConfig.kt` créé ici.
- Fixture test : `class NativeFixture : AutoCloseable` avec `companion object { suspend fun open(): NativeFixture }`, propriétés `session: DeviceSession` et `runtime: DawnRuntime`, `override fun close()` ; ajouter les méthodes raw de création au fil des tâches qui les testent.

- [ ] **Step 1 — test de destruction versus release.**

```kotlin
@Test fun destroyIsIdempotentAndReleaseWaitsForOwner() {
    val calls = mutableListOf<String>()
    val registry = ResourceRegistry()
    val key = Any()
    registry.own(key, destroy = { calls += "destroy" }, release = { calls += "release" })
    registry.destroy(key)
    registry.destroy(key)
    assertEquals(listOf("destroy"), calls)
    registry.close()
    registry.close()
    assertEquals(listOf("destroy", "release"), calls)
}
```

Ajouter le test d'un owner fermé qui refuse nouvelle acquisition, et release
sans destroy pour les objets seulement refcountés. Le test GPU vérifie 20
sessions ouvertes/fermées et une attente interrompue par fermeture du runtime.

- [ ] **Step 2 — Run** tests registry purs, puis `./gradlew :dawn4k:gpuTestJvm --tests '*DawnRuntimeGpuTest'` ; rouge attendu absence runtime.
- [ ] **Step 3 — implémenter discovery et progression.** Réutiliser les bindings et contrats du plan 01, pas les hypothèses de callbacks inline du code wgpu4k historique. Init `WGPUInstanceDescriptor` par defaults ; backend demandé dans `WGPURequestAdapterOptions.backendType`, options compatibles avec le header. Installer callbacks d'erreur et perte device pendant requestDevice. Les CallbackInfo async utilisent `AllowProcessEvents`, les uncaptured-errors sans mode utilisent leur registration répétée. Copier `StringView` et données avant retour natif. Ajouter le pump périodique seulement tant qu'il existe des opérations/erreurs à traiter ; fermer proprement son job et worker.

`ResourceRegistry.destroy` appelle la destruction une fois mais conserve la
référence valide jusqu'au teardown pour les tombstones nécessaires aux tests de
validation. Ses releases arrivent avant DeviceRelease/AdapterRelease/InstanceRelease.
Queue et passes/bundles ont des références possédées même sans close public.
Mapper les errors sur classes implémentant indirectement `GPUValidationError`,
`GPUOutOfMemoryError`, `GPUInternalError`, pas directement `GPUError` sealed.
Une perte device abandonne les attentes avec diagnostic et déclenche terminal
seulement après la preuve d'arrêt des callbacks.

- [ ] **Step 4 — vérifier le vert**, stress pure + GPU sur chaque host ; compteurs internes registrations ouvertes/références restantes égaux à zéro au teardown. Ne pas utiliser un accès à une adresse libérée comme test.
- [ ] **Step 5 — commit** `git add dawn4k` puis `git commit -m "feat(dawn4k): own raw sessions and progress Dawn events"`.

## Task 3: Buffers, mapping et pont ArrayBuffer emprunté

**Files:**
- Create: `dawn4k/src/commonMain/kotlin/org/graphiks/dawn4k/Buffer.kt`.
- Create: `dawn4k/src/commonMain/kotlin/org/graphiks/dawn4k/mapper/BufferDescriptor.kt`.
- Create: `dawn4k/src/commonMain/kotlin/org/graphiks/dawn4k/internal/ArrayBufferInterop.kt`.
- Create: `dawn4k/src/jvmMain/kotlin/org/graphiks/dawn4k/internal/ArrayBufferInterop.jvm.kt`.
- Create: `dawn4k/src/nativeMain/kotlin/org/graphiks/dawn4k/internal/ArrayBufferInterop.native.kt`.
- Create: `dawn4k/src/commonMain/kotlin/org/graphiks/dawn4k/internal/ByteRange.kt`.
- Test: `dawn4k/src/commonTest/kotlin/org/graphiks/dawn4k/BufferGpuTest.kt`.
- Test: `dawn4k/src/commonTest/kotlin/org/graphiks/dawn4k/internal/ByteRangeTest.kt`.
- Modify: `dawn4k/src/commonTest/kotlin/org/graphiks/dawn4k/testing/NativeFixture.kt`.

**Interfaces:**
- `class DawnBuffer internal constructor(session: DeviceSession, handle: WGPUBuffer, descriptor: GPUBufferDescriptor) : GPUBuffer`.
- `data class ByteRange(val offset: ULong, val size: ULong)` ; `fun checkedRange(total: ULong, offset: ULong, size: ULong?): ByteRange`.
- `internal expect fun borrowedArrayBuffer(address: NativeAddress, size: ULong): ArrayBuffer` ; view, pas copie ni ownership de la mémoire.
- Fixture : `fun createBuffer(descriptor: GPUBufferDescriptor): DawnBuffer`, `fun copyAndSubmit(source: DawnBuffer, destination: DawnBuffer, size: ULong)` par encoder raw, avant wrapper CommandEncoder tâche 6.

- [ ] **Step 1 — tests de ranges et de mémoire réelle.**

```kotlin
@Test fun overflowCannotWrapBackInsideTheBuffer() {
    assertFailsWith<IllegalArgumentException> {
        checkedRange(16uL, ULong.MAX_VALUE - 3uL, 8uL)
    }
    assertEquals(ByteRange(4uL, 12uL), checkedRange(16uL, 4uL, null))
}
@Test fun mappedWritesReachReadback() = runTest {
    val fixture = NativeFixture.open()
    try {
        val source = fixture.createBuffer(BufferDescriptor(16uL, GPUBufferUsage.MapWrite or GPUBufferUsage.CopySrc))
        val target = fixture.createBuffer(BufferDescriptor(16uL, GPUBufferUsage.MapRead or GPUBufferUsage.CopyDst))
        try {
            source.mapAsync(GPUMapMode.Write).getOrThrow()
            source.getMappedRange().setUInts(0uL, uintArrayOf(5u, 7u, 11u, 13u))
            source.unmap()
            fixture.copyAndSubmit(source, target, 16uL)
            target.mapAsync(GPUMapMode.Read).getOrThrow()
            assertContentEquals(uintArrayOf(5u, 7u, 11u, 13u), target.getMappedRange().toUIntArray())
            target.unmap()
        } finally { source.close(); target.close() }
    } finally { fixture.close() }
}
```

Cas supplémentaires : mappedAtCreation, map partiel offset=8, remap après unmap,
deux ranges disjoints, range chevauchant, close pending map, map après destroy,
mode Read/Write séparés, tailles de zéro et host conversion >Long.MAX_VALUE.
Pour les contraintes WebGPU (alignement/chevauchement), vérifier l'erreur native,
pas seulement IllegalArgumentException Kotlin.

- [ ] **Step 2 — Run** tests range purs et `:dawn4k:gpuTestJvm --tests '*BufferGpuTest'` ; rouge attendu types/helpers absents.
- [ ] **Step 3 — conversions et wrapper.** Range sans addition qui overflow :

```kotlin
fun checkedRange(total: ULong, offset: ULong, size: ULong?): ByteRange {
    require(offset <= total) { "Offset $offset exceeds $total" }
    val remaining = total - offset
    val count = size ?: remaining
    require(count <= remaining) { "Range size $count exceeds remaining $remaining" }
    return ByteRange(offset, count)
}
```

`DawnBuffer` conserve size/usage/label metadata, mapState suit C, mapAsync utilise
PendingOperation et son Future, close appelle registry.destroy. Écrire
explicitement fields defaults du `WGPU_BUFFER_DESCRIPTOR_INIT`. `getMappedRange`
appelle le vrai C et enveloppe l'adresse via ArrayBuffer.wrap ; JVM
`MemorySegment.ofAddress(rawValue).reinterpret(size.toLong())` après vérification
de representabilité, K/N via `interpretCPointer<ByteVar>` et factory publique.
Zéro/null range ne devient pas un pointeur déréférencé. Des writes dans une
copie CPU suivie d'un upload seraient un faux getMappedRange et sont interdits.

- [ ] **Step 4 — vérifier le vert** pure + GPU JVM/K/N ; les données relues doivent correspondre et les registrations être closes après les chemins d'annulation. Documenter la vue invalide après unmap ; jamais lire sa mémoire après invalidation dans un test K/N.
- [ ] **Step 5 — commit** `git add dawn4k` puis `git commit -m "feat(dawn4k): implement buffer mapping and borrowed memory"`.

## Task 4: Bindings, shaders et pipelines compute autonomes

**Files:**
- Create: `dawn4k/src/commonMain/kotlin/org/graphiks/dawn4k/BindGroup.kt`, `BindGroupLayout.kt`, `PipelineLayout.kt`, `ShaderModule.kt`, `ComputePipeline.kt` (même répertoire).
- Create: `dawn4k/src/commonMain/kotlin/org/graphiks/dawn4k/mapper/BindGroupDescriptor.kt`, `BindGroupLayoutDescriptor.kt`, `PipelineLayoutDescriptor.kt`, `ShaderModuleDescriptor.kt`, `ComputePipelineDescriptor.kt`, `CompilationInfo.kt` (même mapper).
- Test: `dawn4k/src/commonTest/kotlin/org/graphiks/dawn4k/ComputeResourcesGpuTest.kt`.
- Test: `dawn4k/src/commonTest/kotlin/org/graphiks/dawn4k/mapper/BindingsMappingTest.kt`.
- Modify: `dawn4k/src/commonTest/kotlin/org/graphiks/dawn4k/testing/NativeFixture.kt`.

**Interfaces:**
- Wrappers DawnBindGroup/BindGroupLayout/PipelineLayout/ShaderModule/ComputePipeline : constructeur internal `(session: DeviceSession, handle: WGPU<Type>)`, interfaces GPU correspondantes ; label et ownership communs.
- `DawnComputePipeline.getBindGroupLayout(UInt): GPUBindGroupLayout` retourne une référence possédée.
- `DawnShaderModule.getCompilationInfo(): Result<GPUCompilationInfo>` suspend ; copie toutes messages/positions pendant callback.
- Fixture ajoute `createShaderModule(GPUShaderModuleDescriptor): DawnShaderModule`, `createComputePipeline(GPUComputePipelineDescriptor): DawnComputePipeline` et `dispatchAndReadback(pipeline: DawnComputePipeline, expected: UIntArray)` en appels raw pour test autonome.

- [ ] **Step 1 — test compute non trivial.** Shader WGSL :

```wgsl
override scale: u32 = 1u;
@group(0) @binding(0) var<storage, read_write> output: array<u32>;
@compute @workgroup_size(1)
fn main(@builtin(global_invocation_id) id: vec3<u32>) {
    output[id.x] = (id.x + 1u) * scale;
}
```

```kotlin
@Test fun computeConstantsAreApplied() = runTest {
    val fixture = NativeFixture.open()
    try {
        fixture.createShaderModule(ShaderModuleDescriptor(SCALE_SHADER)).use { shader ->
            fixture.createComputePipeline(
                ComputePipelineDescriptor(ProgrammableStage(shader, "main", mapOf("scale" to 3.0)))
            ).use { pipeline ->
                fixture.dispatchAndReadback(pipeline, uintArrayOf(3u, 6u, 9u, 12u))
            }
        }
    } finally { fixture.close() }
}
```

Définir `SCALE_SHADER` dans le test avec le bloc WGSL ci-dessus. Ajouter layout
explicit, auto layout, binding buffer offset/size et dynamic offsets ; cas module
invalid -> compilation diagnostics, async pipeline rejeté -> Result.failure.
Les constantes double sont bit-exactes ; les clés UTF-8 et entryPoint nullable
ne sont pas des pointeurs temporaires libérés trop tôt.

- [ ] **Step 2 — Run** `:dawn4k:gpuTestJvm --tests '*ComputeResourcesGpuTest'` et mapping tests ; rouge absent wrappers/mappers.
- [ ] **Step 3 — implémenter mappers/wrappers.** Shader via chaîne `WGPUShaderSourceWGSL` et son sType, arrays bindings/constants alloués dans le scope consommé, layout null = auto. Les flags Kotlin `.value` ne sont pas supposés identiques aux flags Dawn : table explicite. Les ressources de chaque binding appartiennent au même DeviceSession ; refuser objets d'autre backend avant cast, sans passer de pointer arbitraire. Extraire messages/status puis reprendre sur worker. Fixture dispatch : storage+staging 16 octets, un bind group au layout pipeline, 4 workgroups, copy/map/readback identique à tâche 3. Vérifier que les refs temporaires getBindGroupLayout sont relâchées.
- [ ] **Step 4 — vérifier le vert** dans les trois targets de chaque host ; oracle `[3,6,9,12]`, invalid shader diagnostiqué et async rejection bounded sans sommeil fixe.
- [ ] **Step 5 — commit** `git add dawn4k` puis `git commit -m "feat(dawn4k): implement compute resources and shader diagnostics"`.

## Task 5: Textures, samplers et pipeline render avec oracle offscreen

**Files:**
- Create: `dawn4k/src/commonMain/kotlin/org/graphiks/dawn4k/Texture.kt`, `TextureView.kt`, `Sampler.kt`, `RenderPipeline.kt`, `QuerySet.kt`.
- Create: `dawn4k/src/commonMain/kotlin/org/graphiks/dawn4k/mapper/TextureDescriptor.kt`, `TextureViewDescriptor.kt`, `SamplerDescriptor.kt`, `RenderPipelineDescriptor.kt`, `QuerySetDescriptor.kt`, `Enums.kt`.
- Test: `dawn4k/src/commonTest/kotlin/org/graphiks/dawn4k/RenderResourcesGpuTest.kt`.
- Test: `dawn4k/src/commonTest/kotlin/org/graphiks/dawn4k/mapper/TextureMappingTest.kt`.
- Modify: `dawn4k/src/commonTest/kotlin/org/graphiks/dawn4k/testing/NativeFixture.kt`.

**Interfaces:**
- DawnTexture/TextureView/Sampler/RenderPipeline/QuerySet implémentent GPUTexture/TextureView/Sampler/RenderPipeline/QuerySet ; constructeur internal session+handle, descriptor metadata pour Texture et QuerySet.
- `DawnTexture.createView(GPUTextureViewDescriptor?): GPUTextureView` traduit null par defaults C.
- Fixture ajoute `renderPixel(format: GPUTextureFormat): ByteArray` (pipeline triangle plein écran, texture offscreen RGBA8Unorm, copy vers staging, extraction d'un pixel).

- [ ] **Step 1 — oracle render et null defaults.**

```kotlin
@Test fun offscreenRenderHasAColorOracle() = runTest {
    val fixture = NativeFixture.open()
    try {
        assertContentEquals(byteArrayOf(-1, 0, 0, -1), fixture.renderPixel(GPUTextureFormat.RGBA8Unorm))
    } finally { fixture.close() }
}
```

Shader fragment retourne `vec4f(1.0, 0.0, 0.0, 1.0)` et vertex plein écran ; le
test inspecte un pixel réellement rendu et non la couleur d'un clear. Ajouter
descriptor view null, aspect/depth, mips/layers, sampler compare null,
maxAnisotropy, texture view swizzle feature, booleans depthWriteEnabled nullable.

- [ ] **Step 2 — Run** render GPU + mapping tests ; rouge attendu création ressources non implémentée.
- [ ] **Step 3 — implémenter la traduction complète de cette famille.** Tester/defaults provenant des macros C pour sampleCount, dimension, format,
view mip/layer counts indéfinis, blend ops, depth/stencil et masks. Chaîner
`WGPUTextureComponentSwizzleDescriptor` quand requis ; ne pas demander une
feature optionnelle implicitement. Implémenter samplers et formats par table
exhaustive selon API résolue, sans fallback arbitraire pour enum inconnu.
Render fixture : texture 4x4 RGBA8Unorm, render pass store, shader/pipeline,
draw=3, copy avec bytesPerRow=256 vers staging ; relire le pixel en retirant le
padding, pas tout le buffer en pixels. Les attachments `GPUTexture` doivent
obtenir une vue possédée temporaire ; la conserver durant l'encodage puis la
release au bon propriétaire, pas leak.

- [ ] **Step 4 — vérifier le vert** RGBA oracle + profondeur/stencil/mips tests sur JVM/K/N ; feature swizzle absente n'active pas un test obligatoire en erreur silencieuse.
- [ ] **Step 5 — commit** `git add dawn4k` puis `git commit -m "feat(dawn4k): implement textures samplers and render resources"`.

## Task 6: Queue, encoders, passes, bundles et commandes complètes

**Files:**
- Create: `dawn4k/src/commonMain/kotlin/org/graphiks/dawn4k/Queue.kt`, `CommandEncoder.kt`, `CommandBuffer.kt`, `ComputePassEncoder.kt`, `RenderPassEncoder.kt`, `RenderBundleEncoder.kt`, `RenderBundle.kt`.
- Create: `dawn4k/src/commonMain/kotlin/org/graphiks/dawn4k/mapper/RenderPassDescriptor.kt`, `ComputePassDescriptor.kt`, `TexelCopy.kt`, `RenderBundleDescriptor.kt`, `DataSlice.kt`.
- Test: `dawn4k/src/commonTest/kotlin/org/graphiks/dawn4k/CommandsGpuTest.kt`.
- Test: `dawn4k/src/commonTest/kotlin/org/graphiks/dawn4k/mapper/DataSliceTest.kt`.
- Modify: fixture NativeFixture et DeviceSession (factory de wrappers internes).

**Interfaces:**
- DawnQueue(session, WGPUQueue) : GPUQueue ; DawnCommandEncoder(session, WGPUCommandEncoder) : GPUCommandEncoder ; wrappers passes/bundles/commands aux interfaces homonymes.
- `DawnQueue.submit(List<GPUCommandBuffer>)`, `onSubmittedWorkDone(): Result<Unit>` suspend, writeBuffer/writeTexture signatures exactes de l'API.
- `data class DataSlice(val offset: ULong, val size: ULong)` ; `fun dataSlice(total: ULong, offset: ULong, size: ULong?): DataSlice` réutilise checkedRange.
- Fixture ajoute `createEncoder(): DawnCommandEncoder`, `queue: DawnQueue` ; remplacer les chemins raw dispatch/render des tests par ces wrappers.

- [ ] **Step 1 — tests commands et limites host.**

```kotlin
@Test fun omittedDataSizeMeansRemainingBytes() {
    assertEquals(DataSlice(4uL, 12uL), dataSlice(16uL, 4uL, null))
    assertFailsWith<IllegalArgumentException> { dataSlice(16uL, 17uL, null) }
}
```

Test GPU : writeBuffer avec dataOffset=4 et size=8, copy avec sourceOffset=4,
destinationOffset=8 et size=null, lire sentinelles autour des bytes modifiés.
Ajouter command order, finish twice (validation), command resubmission, destroyed
texture submit, clearBuffer range, writeTexture tight/padded rows, dynamic
offsets empty/list, drawIndexed avec baseVertex=-1 (signed), firstInstance,
indirect dispatch/draw, occlusion/query resolve, render bundle réutilisé.

- [ ] **Step 2 — Run** DataSliceTest + CommandsGpuTest ; rouge attendu méthodes/interfaces absentes.
- [ ] **Step 3 — implémenter toutes les commandes des interfaces.**

| Famille | Traduction/ownership à contrôler |
| --- | --- |
| copies buffers | null size = règle WebGPU restante ; pas unchecked subtraction/ULong overflow |
| uploads | dataOffset/size sont des bytes ; conserver adresse et pin pour durée de l'appel |
| copies textures | offsets/mip/origin/aspect/rows ; ne pas exiger padding 256 pour writeTexture quand C autorise tight rows |
| passes | defaults C, timestamp indices absents avec constante undefined, pass handle release sur end/teardown |
| render | viewport/scissor/blend/stencil, nullable vertex buffer, index format et baseVertex signé |
| bundle | sans close dans interface baseline ; référence libérée par DeviceSession, pas perdue après finish |
| immediates | DataSlice bytes, cap maxImmediateSize, calls SetImmediates réels (pas no-op) |
| maxDrawCount | chaîne WGPURenderPassMaxDrawCount et sType |
| queries | type/count, features requises explicites, resolve offsets et ranges |

Ne pas injecter un état Kotlin qui cache les erreurs de finish twice ou de
resubmission que Dawn doit produire. Les tombstones restent valides selon le
registry. Une mémoire upload non-native doit être pinnée/copied temporairement
selon sa représentation ; éviter le cast `JvmArrayBuffer` dans commonMain.
Le callback workDone suit PendingOperation. Retirer les chemins raw des
fixtures quand wrappers correspondants fonctionnent, pas les bindings eux-mêmes.

- [ ] **Step 4 — vérifier le vert** tests commands + regressions buffer/compute/render sur les quatre configurations ; tous les offsets/sentinelles attendus restent intacts.
- [ ] **Step 5 — commit** `git add dawn4k` puis `git commit -m "feat(dawn4k): implement queue command and pass contracts"`.

## Task 7: Context, Adapter, Device et scopes d'erreurs publics

**Files:**
- Create: `dawn4k/src/commonMain/kotlin/org/graphiks/dawn4k/DawnContext.kt`, `Adapter.kt`, `Device.kt`, `AdapterInfo.kt`, `Limits.kt`, `GPUError.kt`.
- Create: `dawn4k/src/commonMain/kotlin/org/graphiks/dawn4k/mapper/RequestAdapterOptions.kt`, `DeviceDescriptor.kt`, `Limits.kt`, `Features.kt`, `AdapterInfo.kt`.
- Test: `dawn4k/src/commonTest/kotlin/org/graphiks/dawn4k/PublicContractGpuTest.kt`.
- Test: `dawn4k/src/commonTest/kotlin/org/graphiks/dawn4k/mapper/CapabilitiesTest.kt`.
- Modify: DawnRuntime et DeviceSession pour exposer la factory publique sans changer ownership.

**Interfaces:**
- `DawnContext.create(DawnConfig = DawnConfig()): DawnContext` ; `suspend requestAdapter(GPURequestAdapterOptions? = null): Result<GPUAdapter>` ; `suspend fun drainEvents()` pour la validation spécifique Dawn.
- `DawnAdapter(runtime, rawHandle) : GPUAdapter`, propriétés features/limits/info et `suspend requestDevice(GPUDeviceDescriptor?): Result<GPUDevice>`.
- `DawnDevice(session: DeviceSession) : GPUDevice`, toutes les factories raccordées aux wrappers tâches 3–6, scopes/callbacks depuis session.
- Aucun nouvel interface GPU ; ne pas éditer le fichier interfaces.kt dans WebGPU ni en créer une copie ici.

- [ ] **Step 1 — test public utilisant la suite publiée.**

```kotlin
@Test fun publicBuffersMatchTheSharedContract() = runTest {
    val context = DawnContext.create()
    try {
        context.requestAdapter().getOrThrow().use { adapter ->
            adapter.requestDevice().getOrThrow().use { device ->
                org.graphiks.webgpu.suite.acid.buffers.mapWriteRoundTrip(device)
                org.graphiks.webgpu.suite.acid.buffers.mappedAtCreation(device)
            }
        }
    } finally { context.close() }
}
```

Les fonctions ci-dessus existent dans la baseline examinée ; si la snapshot les
renomme, mettre à jour l'import et provenance au lieu de recopier leur ancien
corps. Ajouter adapter fresh, required features présentes/refusées, limites
excessives, empty/nested scopes, callback uncaptured sur device isolé, label
defaultQueue/device et async render/compute success/rejection.

- [ ] **Step 2 — Run** `./gradlew :dawn4k:gpuTestJvm --tests '*PublicContractGpuTest'` ; échec attendu API Context non créée.
- [ ] **Step 3 — implémenter l'entrée publique et les conversions capabilities.** Mapper features par noms/types C explicites. Limites étendues storage par stage via
`WGPUCompatibilityModeLimits` dans la chaîne `WGPULimits`, y compris request.
Ne pas convertir une valeur `WGPU_LIMIT_*_UNDEFINED` en capacité utilisable.
Faire lire la valeur par C quand exposée ; un champ non implémentable impose une
évolution coordonnée du contrat/baseline, pas une capacité inventée.
Les champs de DeviceDescriptor, requiredLimits, defaultQueue et callback ont
des defaults complets. `popErrorScope` copie le message et retourne
Result.success(null) seulement quand C rapporte absence d'erreur. Conversion
exceptions métier vers Result, CancellationException reste une annulation.
FreeMembers après snapshot info/features. Chaque requestAdapter produit une
référence fraîche ; aucun cache partagé avec les cas de contexte.

- [ ] **Step 4 — vérifier le vert** cas ciblés suite publiée + tests ressources précédents, aucune méthode GPUDevice non implémentée. Les classes implémentent directement les interfaces partagées ; leur compilation ordinaire contre une nouvelle snapshot devient l'alarme de changement d'API, sans test de compilation dédié.
- [ ] **Step 5 — commit** `git add dawn4k` puis `git commit -m "feat(dawn4k): expose Graphiks WebGPU adapters and devices"`.

## Task 8: Fermer la couverture des interfaces hors catalogue et documenter les limites

**Files:**
- Create: `dawn4k/src/commonTest/kotlin/org/graphiks/dawn4k/LifecycleStressGpuTest.kt`.
- Create: `docs/docs/architecture.md`, `docs/docs/dependency-updates.md`.
- Create: `inventory/dawn4k-contract.json`.
- Modify: les fichiers de wrapper/mapper dont l'inventaire révèle un trou précis.

**Interfaces:**
- `inventory/dawn4k-contract.json` contient symbol, implementationFile, nativeSymbol, testIds et limitations ; chaque membre de l'API résolue apparaît une fois.
- L'inventaire est un support de revue manuelle de la snapshot réellement résolue, pas du clone master ; aucun test/gate automatisé de coverage structurelle.

- [ ] **Step 1 — relire l'inventaire des interfaces.** Recenser getters/setters, flags/enums et méthodes héritées dans la snapshot résolue. Pour chaque membre, noter implémentation, symbole natif et test fonctionnel ; repérer les trous sans créer de fixture ni test du recensement.
- [ ] **Step 2 — écrire et lancer les tests fonctionnels manquants.** Dans `LifecycleStressGpuTest.kt`, ajouter les cas close répété, callback tardive après cancel et ressources d'un autre device ; exécuter `./gradlew :dawn4k:gpuTestJvm --tests '*LifecycleStressGpuTest'`. Chaque résultat vérifie le cleanup des ressources ou l'erreur native attendue, pas le contenu de l'inventaire.
- [ ] **Step 3 — compléter les témoins non couverts par suite.** Tester setImmediates quand la limite annoncée permet son usage, nullable depthWrite/depthCompare,
et signatures/metadata même quand une feature optionnelle manque. Stress de
100 devices, close répété, callback tardive après cancel, thread étranger,
invalid buffer source et ressources d'un autre device. Les états d'ownership
doivent se terminer sans refs enregistrées. Documenter ArrayBuffer emprunté,
absence de GPUDevice.lost commun, et coordination des mises à jour snapshot :

```sh
./gradlew --refresh-dependencies :dawn4k:compileKotlinJvm :dawn4k:jvmTest
./gradlew :dawn4k:gpuTestJvm
```

Si les nouveaux symboles exigent un changement WebGPU ou Dawn-packer, ouvrir un
incrément amont, consommer la nouvelle snapshot/release et relancer les gates ;
ne pas reporter une méthode obligatoire comme unsupported pour rendre le rapport vert.

- [ ] **Step 4 — vérifier le vert** regressions quatre configurations et stress, puis revue d'inventaire sans méthode obligatoire manquante. Test du compteur de cleanup n'est pas une preuve complète d'absence de fuite native ; indiquer cette limite.
- [ ] **Step 5 — commit** `git add dawn4k inventory docs/docs/architecture.md docs/docs/dependency-updates.md` puis `git commit -m "test(dawn4k): cover public interfaces and resource lifecycle"`.

## Gate de fin de plan

- [ ] Aucun `NotImplementedError`, stub no-op ou false-success dans les méthodes du contrat public.
- [ ] Readbacks buffer/compute/render corrects sur JVM et K/N des deux hosts.
- [ ] Types publics compilés contre WebGPU 0.1.0-SNAPSHOT réellement résolue.
- [ ] Annulation, destruction, scopes et erreurs de pipeline ont des tests positifs/négatifs.
- [ ] Revue ownership/callbacks avant le plan 03 ; pas de publication distante automatique.
