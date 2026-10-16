package org.graphiks.dawn4k.toolkit

import kotlinx.coroutines.runBlocking
import kotlin.test.*

class HeadlessClasspathTest {
    @Test fun headlessLoadsNoWindowToolkit() = runBlocking<Unit> {
        withDawnSession {
            assertFailsWith<ClassNotFoundException> { Class.forName("org.lwjgl.glfw.GLFW", false, javaClass.classLoader) }
        }
    }
}
