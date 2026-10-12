package org.graphiks.dawn4k.demo

import android.view.Surface

/** JNI returns one retained ANativeWindow reference, never a Java object address. */
internal object AndroidNativeWindow {
    init { System.loadLibrary("dawn4k_demo_window") }
    external fun acquire(surface: Surface?): Long
    external fun release(address: Long)
}
