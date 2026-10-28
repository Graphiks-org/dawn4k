package org.graphiks.dawn4k.toolkit.examples

import kotlinx.coroutines.runBlocking
import org.graphiks.dawn4k.toolkit.DawnSessionOptions
import org.graphiks.dawn4k.toolkit.withDawnSession
import kotlin.time.Duration.Companion.seconds

/** Shared by the JVM and native desktop launchers; invoke on process main. */
fun computeMain() = runBlocking {
    withDawnSession(DawnSessionOptions(startupTimeout = 60.seconds)) {
        println(runComputeExample(it).contentToString())
    }
}
