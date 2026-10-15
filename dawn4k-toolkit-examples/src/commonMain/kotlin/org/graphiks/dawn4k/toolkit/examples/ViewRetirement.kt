package org.graphiks.dawn4k.toolkit.examples

import kotlinx.coroutines.*

/** Invoke from the UI callback before cancelling its scope; cleanup remains a child until native acknowledgment. */
fun CoroutineScope.launchViewRetirement(rendering: Job?, retire: suspend () -> Unit): Job =
    launch(start = CoroutineStart.UNDISPATCHED) {
        withContext(NonCancellable) {
            rendering?.cancelAndJoin()
            retire()
        }
    }
