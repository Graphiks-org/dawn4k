# kffi receives each trampoline Class and looks up this static JNI entry by name
# and signature. Preserve the entry; unused trampoline classes may still shrink.
-keepclassmembers class org.graphiks.dawn4k.native.*Trampoline {
    public static void dispatch(...);
}
