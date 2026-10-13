package org.graphiks.dawn4k.acid

import org.graphiks.dawn4k.DawnBackend

internal data class AcidSettings(val backend: DawnBackend?, val requireAdapter: Boolean) {
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
