package org.graphiks.dawn4k.webgpu.toolkit.target

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.graphiks.dawn4k.toolkit.surface.DawnPresentationOptions
import org.graphiks.dawn4k.toolkit.surface.DawnSurfaceTarget
import org.graphiks.dawn4k.toolkit.surface.DawnTargetLease
import org.graphiks.dawn4k.toolkit.surface.PixelExtent
import org.graphiks.webgpu.toolkit.Extent2D
import org.graphiks.webgpu.toolkit.SurfaceTarget
import org.graphiks.webgpu.toolkit.TargetLease
import org.graphiks.webgpu.toolkit.TargetState

/**
 * Dawn4k implementation of [SurfaceTarget] for native platform surfaces.
 * 
 * This target wraps a native surface (Metal, Win32, X11, Wayland) and provides
 * lifecycle management through generations, extent tracking, and lease-based
 * retention for drawing operations.
 * 
 * @param dawnTarget The dawn4k toolkit target this contract target fronts, when the surface
 *   comes from a native window; presentation goes through it
 * @param platformType The platform-specific surface type
 * @param initialExtent The initial physical extent of the surface
 * @param presentationOptions The dawn4k negotiation policy the presentation opened for this
 *   target applies; the toolkit's own policy is translated into it
 */
class Dawn4kSurfaceTarget(
    private val dawnTarget: DawnSurfaceTarget?,
    val platformType: PlatformSurfaceType,
    initialExtent: Extent2D,
    internal val presentationOptions: DawnPresentationOptions = DawnPresentationOptions(),
) : SurfaceTarget {
    
    private val mutex = Mutex()
    private val _state = MutableStateFlow(
        TargetState(
            generation = 1L,
            extent = initialExtent,
            available = initialExtent.width > 0u && initialExtent.height > 0u
        )
    )
    
    override val state: StateFlow<TargetState> = _state.asStateFlow()
    
    private var currentGeneration: Long = 1L
    private var currentExtent: Extent2D = initialExtent
    private var isAvailable: Boolean = initialExtent.width > 0u && initialExtent.height > 0u
    private val activeLeases = mutableSetOf<Dawn4kTargetLease>()
    
    /**
     * Updates the surface extent and generation. This should be called when the
     * native surface is resized or recreated.
     */
    suspend fun updateExtent(newExtent: Extent2D) = mutex.withLock {
        val newGeneration = currentGeneration + 1L
        val newAvailable = newExtent.width > 0u && newExtent.height > 0u
        
        currentGeneration = newGeneration
        currentExtent = newExtent
        isAvailable = newAvailable
        
        _state.value = TargetState(
            generation = newGeneration,
            extent = newExtent,
            available = newAvailable
        )
        
        // Retire all active leases since the surface changed
        activeLeases.forEach { lease ->
            lease.markStale()
        }
        activeLeases.clear()
    }
    
    /**
     * Marks this surface as unavailable (e.g., when minimized or occluded).
     */
    suspend fun setAvailable(available: Boolean) = mutex.withLock {
        isAvailable = available
        _state.value = TargetState(
            generation = currentGeneration,
            extent = currentExtent,
            available = available
        )
    }
    
    override suspend fun retain(): TargetLease {
        // A target that fronts a dawn4k surface retains that surface, so the lease carries the
        // native reference the presentation needs and the dawn4k generations stay authoritative.
        val native = dawnTarget
        if (native != null) {
            val lease = native.retain()
            return Dawn4kTargetLease(
                generation = lease.state.value.generation,
                extent = lease.state.value.extent.toExtent2D(),
                dawnTarget = native,
                surfaceTarget = this,
            ).also { it.attachNativeLease(lease) }
        }
        return retainHeadless()
    }

    private suspend fun retainHeadless(): TargetLease = mutex.withLock {
        // Check if surface is available and has valid extent
        if (!isAvailable || currentExtent.width == 0u || currentExtent.height == 0u) {
            throw IllegalStateException("Cannot retain surface: not available or extent is zero")
        }
        
        val lease = Dawn4kTargetLease(
            generation = currentGeneration,
            extent = currentExtent,
            dawnTarget = dawnTarget,
            surfaceTarget = this
        )
        activeLeases.add(lease)
        return lease
    }
    
    override suspend fun retire(lease: TargetLease) {
        if (lease !is Dawn4kTargetLease) return
        val native = lease.nativeLease
        if (native != null) {
            // Tolerant: retiring a stale dawn4k lease is a no-op there as well.
            runCatching { native.retire() }
            lease.markRetired()
            return
        }
        mutex.withLock {
            // Check if lease is from this target and current generation
            if (lease.generation != currentGeneration) {
                // Stale lease - no-op
                return@withLock
            }

            // Remove from active leases
            activeLeases.remove(lease)
            lease.markRetired()
        }
    }
    
    /** The dawn4k target backing this contract target, or null for a headless target. */
    internal val nativeTarget: DawnSurfaceTarget? get() = dawnTarget
    
    private var closed = false

    /**
     * Closes this surface target and releases resources.
     * Idempotent: closing an already-closed target is a no-op.
     */
    fun close() {
        if (closed) return
        closed = true
    }
}

/**
 * Dawn4k implementation of [TargetLease] for native surfaces.
 */
class Dawn4kTargetLease(
    override val generation: Long,
    val extent: Extent2D,
    val dawnTarget: DawnSurfaceTarget?,
    val surfaceTarget: Dawn4kSurfaceTarget,
) : TargetLease {

    /** The dawn4k lease this contract lease fronts, when the target presents to a native surface. */
    internal var nativeLease: DawnTargetLease? = null
        private set

    internal fun attachNativeLease(lease: DawnTargetLease) {
        nativeLease = lease
    }
    
    private var isStale = false
    private var isRetired = false
    
    /**
     * Marks this lease as stale (surface was resized/recreated).
     */
    internal fun markStale() {
        isStale = true
    }
    
    /**
     * Marks this lease as retired.
     */
    internal fun markRetired() {
        isRetired = true
    }
    
    /**
     * Returns true if this lease is still valid for use.
     */
    fun isValid(): Boolean = !isStale && !isRetired
}

internal fun PixelExtent.toExtent2D(): Extent2D = Extent2D(width.toUInt(), height.toUInt())
