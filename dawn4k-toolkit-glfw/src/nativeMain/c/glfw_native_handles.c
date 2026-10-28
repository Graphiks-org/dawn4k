#include "glfw_native_handles.h"
#if defined(__APPLE__)
#define GLFW_EXPOSE_NATIVE_COCOA
#elif defined(__linux__)
#define GLFW_EXPOSE_NATIVE_X11
#define GLFW_EXPOSE_NATIVE_WAYLAND
#endif
#include <GLFW/glfw3native.h>

int dawn_glfw_platform(void) { return glfwGetPlatform(); }
void* dawn_glfw_cocoa_window(GLFWwindow* window) {
#if defined(__APPLE__)
    if (glfwGetPlatform() == GLFW_PLATFORM_COCOA) return (void*)glfwGetCocoaWindow(window);
#else
    (void)window;
#endif
    return 0;
}
void* dawn_glfw_x11_display(void) {
#if defined(__linux__)
    if (glfwGetPlatform() == GLFW_PLATFORM_X11) return glfwGetX11Display();
#endif
    return 0;
}
uint64_t dawn_glfw_x11_window(GLFWwindow* window) {
#if defined(__linux__)
    if (glfwGetPlatform() == GLFW_PLATFORM_X11) return (uint64_t)glfwGetX11Window(window);
#else
    (void)window;
#endif
    return 0;
}
void* dawn_glfw_wayland_display(void) {
#if defined(__linux__)
    if (glfwGetPlatform() == GLFW_PLATFORM_WAYLAND) return glfwGetWaylandDisplay();
#endif
    return 0;
}
void* dawn_glfw_wayland_window(GLFWwindow* window) {
#if defined(__linux__)
    if (glfwGetPlatform() == GLFW_PLATFORM_WAYLAND) return glfwGetWaylandWindow(window);
#else
    (void)window;
#endif
    return 0;
}
