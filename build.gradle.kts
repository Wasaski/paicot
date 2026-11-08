// Top-level build file for PAICOT.
// Plugin versions are pinned to a known-good stable set:
// AGP 8.5.2 + Gradle 8.7 + Kotlin 2.0.20 + JDK 17 + Chaquopy 17.2.0.
plugins {
    id("com.android.application") version "8.5.2" apply false
    id("org.jetbrains.kotlin.android") version "2.0.20" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.20" apply false
    id("com.chaquo.python") version "17.2.0" apply false
    id("com.google.devtools.ksp") version "2.0.20-1.0.25" apply false
}
