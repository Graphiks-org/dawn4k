package org.graphiks.dawn4k.toolkit.glfw.internal

import java.lang.foreign.FunctionDescriptor
import java.lang.foreign.Linker
import java.lang.foreign.ValueLayout.JAVA_INT

internal actual fun requireGlfwMainThread() {
    val os = System.getProperty("os.name").lowercase()
    val lookup = Linker.nativeLinker().defaultLookup()
    fun call(name: String): Int {
        val address = lookup.find(name).orElseThrow { IllegalStateException("missing main-thread check: $name") }
        return Linker.nativeLinker().downcallHandle(address, FunctionDescriptor.of(JAVA_INT)).invokeWithArguments() as Int
    }
    val main = when {
        "mac" in os -> call("pthread_main_np") != 0
        else -> {
            @Suppress("DEPRECATION")
            val id = Thread.currentThread().id
            id == 1L
        }
    }
    check(main) { "GLFW requires the process main thread; macOS JVM launchers need -XstartOnFirstThread" }
}
internal actual fun pauseGlfwMainThread() { Thread.sleep(1) }
