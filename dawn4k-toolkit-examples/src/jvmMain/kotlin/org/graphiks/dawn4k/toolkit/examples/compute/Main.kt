package org.graphiks.dawn4k.toolkit.examples.compute

import kotlinx.coroutines.runBlocking
import org.graphiks.dawn4k.toolkit.withDawnSession
import org.graphiks.dawn4k.toolkit.examples.runComputeExample

fun main() = runBlocking {
    withDawnSession { gpu -> println(runComputeExample(gpu).contentToString()) }
}
