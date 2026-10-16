package org.graphiks.dawn4k.toolkit.examples.compute

import kotlinx.coroutines.runBlocking
import org.graphiks.dawn4k.toolkit.DawnSessionOptions
import org.graphiks.dawn4k.toolkit.withDawnSession
import org.graphiks.dawn4k.toolkit.examples.runComputeExample
import kotlin.time.Duration.Companion.seconds

fun main() = runBlocking {
    withDawnSession(DawnSessionOptions(startupTimeout = 60.seconds)) { gpu ->
        println(runComputeExample(gpu).contentToString())
    }
}
