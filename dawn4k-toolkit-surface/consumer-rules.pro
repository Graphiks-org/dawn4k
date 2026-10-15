# JNI names in android_window.c must survive consumer R8 shrinking/obfuscation.
-keep,allowoptimization class org.graphiks.dawn4k.toolkit.surface.AndroidNativeWindow {
    native <methods>;
}
