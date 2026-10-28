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
            // Java 25 HotSpot reserves IDs 1 and 2; its primordial thread has ID 3.
            // Do not use the mutable thread name: a worker can also be named "main".
            Thread.currentThread().threadId() == 3L
        }
    }
    check(main) { "GLFW requires the process main thread; macOS JVM launchers need -XstartOnFirstThread" }
}
internal actual fun pauseGlfwMainThread() { Thread.sleep(1) }
