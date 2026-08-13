# Ktor
-keep class io.ktor.** { *; }
-dontwarn io.ktor.**

# kotlinx.serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.json.** { kotlinx.serialization.KSerializer serializer(...); }
-keep,includedescriptorclasses class com.divafinance.**$$serializer { *; }
-keepclassmembers class com.divafinance.** { *** Companion; }
-keepclasseswithmembers class com.divafinance.** { kotlinx.serialization.KSerializer serializer(...); }

# SQLDelight
-keep class app.cash.sqldelight.** { *; }
