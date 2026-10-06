package org.graphiks.dawn4k

/**
 * Runtime creation options.
 *
 * A null [backend] requests the default adapter; a requested backend is carried
 * into the native adapter request options.
 */
data class DawnConfig(val backend: DawnBackend? = null)

/** The native backends Dawn can be asked for explicitly. */
enum class DawnBackend { Metal, Vulkan, D3D12 }
