plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.ovelin.space"
    compileSdk = 35
    ndkVersion = "27.0.12077973"

    defaultConfig {
        applicationId = "com.ovelin.space.bit64"
        minSdk = 26
        // The virtualization engine needs the host to target API 28 (it relies on pre-API-29
        // behaviour such as executing code from app-private storage). Sideload-only; not Play-eligible.
        targetSdk = 28
        versionCode = 16
        versionName = "0.4.2"
        // A single Android process (and every sub-process it forks for a virtual instance) is
        // either 32-bit or 64-bit for its whole life — the OS picks that once, from the installed
        // package's own native libraries, and it cannot be mixed within one running app. There is
        // no public Android API to make one install run both at once, on any device, rooted or not.
        // This project is the 64-bit build ("Iskander 64"); a separate project builds the 32-bit
        // one ("Iskander 32"). Different applicationId so both can be installed at the same time.
    }

    // This project builds ONLY the 64-bit (arm64-v8a) APK: "Iskander 64".
    // Use apps on it that the clone screen marks as 64-bit-only (or that work either way).
    defaultConfig {
        ndk {
            abiFilters += listOf("arm64-v8a")
        }
    }

    // Fixed debug key committed with the project. CI otherwise generates a random debug key on every
    // run, and Android refuses to update an installed app signed with a different key.
    signingConfigs {
        getByName("debug") {
            storeFile = file("ovelin-debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
        // android:debuggable="true" is baked into every "debug" build by default, and it's a real,
        // system-wide tax on the ART runtime hosting EVERY process of this app - including every
        // cloned app (Facebook included), not just our own UI. That's a very plausible reason
        // scrolling/navigation/buttons feel uniformly sluggish across the board rather than in one
        // specific feature. Turning it off keeps everything else about the "debug" build (same
        // task name `assembleDebug`, same debug keystore, no risky code-shrinking) so the CI
        // workflow and the build script need no changes, but every virtual process now runs with
        // normal, non-debuggable ART performance characteristics.
        getByName("debug") {
            isDebuggable = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    kotlinOptions {
        jvmTarget = "21"
    }

    packaging {
        jniLibs { useLegacyPackaging = true }
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(project(":Bcore"))
    val composeBom = platform("androidx.compose:compose-bom:2025.04.01")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.room:room-runtime:2.7.1")
    implementation("androidx.room:room-ktx:2.7.1")
    ksp("androidx.room:room-compiler:2.7.1")

    debugImplementation("androidx.compose.ui:ui-tooling")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
}
