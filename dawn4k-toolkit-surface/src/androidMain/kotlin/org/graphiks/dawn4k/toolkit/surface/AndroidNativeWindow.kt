package org.graphiks.dawn4k.toolkit.surface

import android.view.Surface

/** Each successful acquire returns an owned ANativeWindow reference. */
internal object AndroidNativeWindow {
    init { System.loadLibrary("dawn4k_toolkit_window") }
    external fun acquire(surface: Surface?): Long
    external fun release(address: Long)
    external fun width(address: Long): Int
}
