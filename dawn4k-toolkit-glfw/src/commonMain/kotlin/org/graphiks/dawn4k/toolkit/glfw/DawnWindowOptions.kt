package org.graphiks.dawn4k.toolkit.glfw

enum class DawnGlfwRuntimeOwnership { Owned, Borrowed }

data class DawnWindowOptions(
    val title: String = "Dawn4k",
    val width: Int = 1280,
    val height: Int = 720,
    val visible: Boolean = true,
    val resizable: Boolean = true,
    val runtimeOwnership: DawnGlfwRuntimeOwnership = DawnGlfwRuntimeOwnership.Owned,
) {
    init { require(width > 0 && height > 0) { "initial window dimensions must be positive" } }
}
