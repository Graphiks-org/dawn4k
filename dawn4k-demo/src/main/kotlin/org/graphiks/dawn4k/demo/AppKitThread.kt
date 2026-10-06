package org.graphiks.dawn4k.demo

import java.lang.foreign.Arena
import java.lang.foreign.FunctionDescriptor
import java.lang.foreign.Linker
import java.lang.foreign.MemorySegment
import java.lang.foreign.ValueLayout
import java.lang.invoke.MethodHandles
import java.lang.invoke.MethodType
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicLong
import org.graphiks.kffi.objc.CFRelease
import org.graphiks.kffi.objc.CFRunLoopAddSource
import org.graphiks.kffi.objc.CFRunLoopGetMain
import org.graphiks.kffi.objc.CFRunLoopRemoveSource
import org.graphiks.kffi.objc.CFRunLoopSourceContext
import org.graphiks.kffi.objc.CFRunLoopSourceCreate
import org.graphiks.kffi.objc.CFRunLoopSourceInvalidate
import org.graphiks.kffi.objc.CFRunLoopSourceSignal
import org.graphiks.kffi.objc.CFRunLoopWakeUp
import org.graphiks.kffi.objc.kCFRunLoopCommonModes
import org.graphiks.kffi.objc.NSThread
import org.graphiks.kffi.objc.ObjCRuntime
import org.graphiks.kffi.objc.PlatformAvailability

/**
 * AWT's EDT is not AppKit's thread. A common-mode source also runs during mouse
 * tracking/live resize, unlike dispatch_sync on the main queue, which stalls there.
 */
@OptIn(PlatformAvailability::class)
internal fun <T> onAppKitThread(block: () -> T): T {
    if (NSThread.isMainThread()) return ObjCRuntime.autoreleasePool(block)
    val task = AppKitTask { ObjCRuntime.autoreleasePool(block) }
    AppKitDispatch.run(task)
    @Suppress("UNCHECKED_CAST")
    return task.result!!.getOrThrow() as T
}

private class AppKitTask(private val block: () -> Any?) {
    var result: Result<Any?>? = null
    val completed = CountDownLatch(1)

    fun invoke() {
        // Never unwind a Java exception across an FFM upcall into CoreFoundation.
        result = runCatching(block)
        completed.countDown()
    }
}

private object AppKitDispatch {
    private val ids = AtomicLong()
    private val tasks = ConcurrentHashMap<Long, AppKitTask>()
    // The trampoline outlives all calls: signalling completion must not free an
    // upcall stub while its native invocation is still returning.
    private val callback = Linker.nativeLinker().upcallStub(
        MethodHandles.lookup().findVirtual(
            AppKitDispatch::class.java, "perform",
            MethodType.methodType(Void.TYPE, MemorySegment::class.java),
        ).bindTo(this),
        FunctionDescriptor.ofVoid(ValueLayout.ADDRESS), Arena.global(),
    )

    fun perform(info: MemorySegment) {
        tasks[info.address()]?.invoke()
    }

    fun run(task: AppKitTask) {
        val id = ids.incrementAndGet()
        tasks[id] = task
        var source = MemorySegment.NULL
        var interrupted = false
        val loop = CFRunLoopGetMain()
        try {
            source = Arena.ofConfined().use { arena ->
                val context = CFRunLoopSourceContext.allocate(arena)
                CFRunLoopSourceContext().apply {
                    info(context, MemorySegment.ofAddress(id))
                    perform(context, callback)
                }
                CFRunLoopSourceCreate(MemorySegment.NULL, 0L, context)
            }
            check(source != MemorySegment.NULL) { "CFRunLoopSourceCreate failed" }
            CFRunLoopAddSource(loop, source, kCFRunLoopCommonModes)
            CFRunLoopSourceSignal(source)
            CFRunLoopWakeUp(loop)
            // Match a synchronous native call: keep resources alive until the
            // callback finishes, even if the waiting Java thread is interrupted.
            while (true) {
                try {
                    task.completed.await()
                    break
                } catch (_: InterruptedException) {
                    interrupted = true
                }
            }
        } finally {
            if (source != MemorySegment.NULL) {
                CFRunLoopSourceInvalidate(source)
                CFRunLoopRemoveSource(loop, source, kCFRunLoopCommonModes)
                CFRelease(source)
            }
            tasks.remove(id)
            if (interrupted) Thread.currentThread().interrupt()
        }
    }
}
