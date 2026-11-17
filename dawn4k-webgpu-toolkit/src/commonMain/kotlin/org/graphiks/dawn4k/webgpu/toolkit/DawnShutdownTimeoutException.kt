package org.graphiks.dawn4k.webgpu.toolkit

/** Live handles and their execution owner remain available for a later shutdown attempt. */
class DawnShutdownTimeoutException(cause: Throwable) : IllegalStateException(
    "Dawn shutdown timed out; stop remaining resource users and retry shutdown", cause,
)
