plugins {
    id("com.android.application") version "8.9.2" apply false
    id("org.jetbrains.kotlin.android") version "2.1.20" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.1.20" apply false
    id("com.google.devtools.ksp") version "2.1.20-1.0.32" apply false
}

// Read by the :Bcore virtualization module (Groovy: rootProject.ext.*).
extra["compileSdkVersion"] = 35
extra["minSdk"] = 26
