package org.graphiks.dawn4k.testing

internal actual fun gpuTestEnv(name: String): String? = System.getenv(name)

internal actual fun gpuTestNotExecuted(warning: String) {
    throw org.junit.AssumptionViolatedException(warning)
}
