#ifndef DAWN_GLFW_NATIVE_HANDLES_H
#define DAWN_GLFW_NATIVE_HANDLES_H
#define GLFW_INCLUDE_NONE
#include <GLFW/glfw3.h>
#include <stdint.h>
#ifdef __cplusplus
extern "C" {
#endif
int dawn_glfw_platform(void);
void* dawn_glfw_cocoa_window(GLFWwindow* window);
void* dawn_glfw_x11_display(void);
uint64_t dawn_glfw_x11_window(GLFWwindow* window);
void* dawn_glfw_wayland_display(void);
void* dawn_glfw_wayland_window(GLFWwindow* window);
#ifdef __cplusplus
}
#endif
#endif
