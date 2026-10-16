#include <jni.h>
#include <stdint.h>
#include <android/native_window_jni.h>

JNIEXPORT jlong JNICALL Java_org_graphiks_dawn4k_toolkit_surface_AndroidNativeWindow_acquire(
    JNIEnv *env, jobject self, jobject surface) {
    (void)self;
    if (!surface) return 0;
    return (jlong)(uintptr_t)ANativeWindow_fromSurface(env, surface);
}

JNIEXPORT void JNICALL Java_org_graphiks_dawn4k_toolkit_surface_AndroidNativeWindow_release(
    JNIEnv *env, jobject self, jlong address) {
    (void)env;
    (void)self;
    if (address) ANativeWindow_release((ANativeWindow *)(uintptr_t)address);
}

JNIEXPORT jint JNICALL Java_org_graphiks_dawn4k_toolkit_surface_AndroidNativeWindow_width(
    JNIEnv *env, jobject self, jlong address) {
    (void)env;
    (void)self;
    return address ? ANativeWindow_getWidth((ANativeWindow *)(uintptr_t)address) : 0;
}
