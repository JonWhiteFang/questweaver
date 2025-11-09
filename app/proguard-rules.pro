# Keep models for kotlinx-serialization
-keepclassmembers class ** {
    @kotlinx.serialization.Serializable *;
}
-keepattributes *Annotation*
