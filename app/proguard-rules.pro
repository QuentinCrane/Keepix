# Keep rules kept intentionally small: release size is mainly reduced by R8 code/resource shrinking.
# Room, Hilt and Compose generate their own metadata through Gradle plugins.

# Preserve app entry point names for easier crash-log reading.
-keep class com.futureape.kanleme.KanlemeApplication { *; }
-keep class com.futureape.kanleme.MainActivity { *; }

# MediaStore/EXIF helpers sometimes surface reflective warnings through platform code; do not warn on them.
-dontwarn androidx.exifinterface.**
