package org.graphiks.dawn4k.webgpu.toolkit.interop

import org.graphiks.dawn4k.webgpu.toolkit.surface.DawnAlphaMode
import org.graphiks.dawn4k.webgpu.toolkit.surface.DawnPresentMode
import org.graphiks.dawn4k.webgpu.toolkit.surface.DawnPresentationOptions
import org.graphiks.webgpu.GPUTextureFormat
import org.graphiks.webgpu.toolkit.AlphaMode
import org.graphiks.webgpu.toolkit.PresentMode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Tests for the translation between dawn4k's [DawnPresentationOptions] and the toolkit's
 * `PresentationPolicy`, which is what makes `withDawnWindow` and `replaceTarget` honour the
 * caller's policy instead of negotiating on their own.
 */
class DawnPresentationPolicyTranslationTest {

    @Test
    fun `every axis is carried over with its required and preferred values`() {
        val policy = DawnPresentationOptions(
            preferredFormats = listOf(GPUTextureFormat.RGBA8Unorm, GPUTextureFormat.BGRA8Unorm),
            requiredFormat = GPUTextureFormat.RGBA8Unorm,
            preferredPresentModes = listOf(DawnPresentMode.Mailbox, DawnPresentMode.Fifo),
            requiredPresentMode = DawnPresentMode.Mailbox,
            preferredAlphaModes = listOf(DawnAlphaMode.Premultiplied, DawnAlphaMode.Opaque),
            requiredAlphaMode = DawnAlphaMode.Premultiplied,
        ).toPresentationPolicy()

        assertEquals(listOf(GPUTextureFormat.RGBA8Unorm, GPUTextureFormat.BGRA8Unorm), policy.preferredFormats)
        assertEquals(GPUTextureFormat.RGBA8Unorm, policy.requiredFormat)
        assertEquals(listOf(PresentMode.Mailbox, PresentMode.Fifo), policy.preferredPresentModes)
        assertEquals(PresentMode.Mailbox, policy.requiredPresentMode)
        assertEquals(listOf(AlphaMode.Premultiplied, AlphaMode.Opaque), policy.preferredAlphaModes)
        assertEquals(AlphaMode.Premultiplied, policy.requiredAlphaMode)
    }

    @Test
    fun `every present mode has a toolkit counterpart`() {
        val policy = DawnPresentationOptions(
            preferredPresentModes = DawnPresentMode.entries,
        ).toPresentationPolicy()

        assertEquals(
            listOf(PresentMode.Fifo, PresentMode.FifoRelaxed, PresentMode.Immediate, PresentMode.Mailbox),
            policy.preferredPresentModes,
        )
    }

    @Test
    fun `unpremultiplied maps to the toolkit's post-multiplied mode`() {
        val policy = DawnPresentationOptions(
            preferredAlphaModes = listOf(DawnAlphaMode.Unpremultiplied),
        ).toPresentationPolicy()

        assertEquals(listOf(AlphaMode.PostMultiplied), policy.preferredAlphaModes)
    }

    @Test
    fun `auto and inherit alpha modes are dropped since the toolkit cannot name them`() {
        val policy = DawnPresentationOptions(
            preferredAlphaModes = listOf(
                DawnAlphaMode.Auto,
                DawnAlphaMode.Inherit,
                DawnAlphaMode.Opaque,
            ),
            requiredAlphaMode = DawnAlphaMode.Inherit,
        ).toPresentationPolicy()

        assertEquals(listOf(AlphaMode.Opaque), policy.preferredAlphaModes)
        assertNull(policy.requiredAlphaMode)
    }

    @Test
    fun `the default dawn4k options translate to a policy with no required value`() {
        val policy = DawnPresentationOptions().toPresentationPolicy()

        assertNull(policy.requiredFormat)
        assertNull(policy.requiredAlphaMode)
        assertNull(policy.requiredPresentMode)
        assertEquals(listOf(PresentMode.Fifo), policy.preferredPresentModes)
    }
}
