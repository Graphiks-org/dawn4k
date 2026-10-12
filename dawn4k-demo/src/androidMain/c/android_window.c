#include <jni.h>
#include <stdint.h>
#include <android/native_window_jni.h>

JNIEXPORT jlong JNICALL Java_org_graphiks_dawn4k_demo_AndroidNativeWindow_acquire(
    JNIEnv *env, jobject self, jobject surface) {
    (void)self;
    if (!surface) return 0;
    ANativeWindow *window = ANativeWindow_fromSurface(env, surface);
    return (jlong)(uintptr_t)window;
}

JNIEXPORT void JNICALL Java_org_graphiks_dawn4k_demo_AndroidNativeWindow_release(
    JNIEnv *env, jobject self, jlong address) {
    (void)env;
    (void)self;
    if (address) ANativeWindow_release((ANativeWindow *)(uintptr_t)address);
}
