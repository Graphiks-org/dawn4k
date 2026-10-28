package ygdrasil.conventions

import kotlin.test.*

class LinuxTargetGateTest {
    @Test fun matchingLinuxHostDeclaresTargetWithoutBundles() {
        assertTrue(linuxX64TargetEnabled("Linux", "amd64", dawnBundle = null, glfwBundle = null))
        assertTrue(linuxX64TargetEnabled("linux", "x86_64", dawnBundle = null, glfwBundle = null))
    }

    @Test fun foreignHostRequiresBothVerifiedBundles() {
        assertFalse(linuxX64TargetEnabled("Mac OS X", "aarch64", dawnBundle = null, glfwBundle = null))
        assertFalse(linuxX64TargetEnabled("Mac OS X", "aarch64", dawnBundle = "/bundles/dawn", glfwBundle = null))
        assertFalse(linuxX64TargetEnabled("Mac OS X", "aarch64", dawnBundle = null, glfwBundle = "/bundles/glfw"))
        assertFalse(linuxX64TargetEnabled("Windows 11", "amd64", dawnBundle = null, glfwBundle = null))
        assertTrue(linuxX64TargetEnabled("Mac OS X", "aarch64", dawnBundle = "/bundles/dawn", glfwBundle = "/bundles/glfw"))
        assertTrue(linuxX64TargetEnabled("Windows 11", "amd64", dawnBundle = "/bundles/dawn", glfwBundle = "/bundles/glfw"))
    }

    @Test fun linuxArm64HostNeedsBundlesLikeAnyForeignHost() {
        assertFalse(linuxX64TargetEnabled("Linux", "aarch64", dawnBundle = null, glfwBundle = null))
        assertTrue(linuxX64TargetEnabled("Linux", "aarch64", dawnBundle = "/bundles/dawn", glfwBundle = "/bundles/glfw"))
    }
}
