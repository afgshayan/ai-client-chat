# Keep model classes used for JSON (kotlinx.serialization) reflection
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

-keepclasseswithmembers class com.aiclient.chat.data.remote.** {
    *** Companion;
}
-keepclasseswithmembers class com.aiclient.chat.data.remote.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.aiclient.chat.data.remote.**$$serializer { *; }
-keepclassmembers class com.aiclient.chat.data.remote.** {
    *** Companion;
}

# OkHttp / Okio
-dontwarn okhttp3.**
-dontwarn okio.**
