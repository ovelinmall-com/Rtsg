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
        applicationId = "com.ovelin.space"
        minSdk = 26
        // The virtualization engine needs the host to target API 28 (it relies on pre-API-29
        // behaviour such as executing code from app-private storage). Sideload-only; not Play-eligible.
        targetSdk = 28
        versionCode = 6
        versionName = "0.3.1"
        // Single 32-bit build. Packaging ONLY armeabi-v7a native libs forces Android to always run
        // this app's process as 32-bit (zygote picks the bitness from the installed package's native
        // libs, not from the device). That covers the overwhelming majority of cloned apps — most
        // Play Store apps still ship 32-bit code — without the user juggling two APK variants.
        // Trade-off: an app that ships ONLY 64-bit native libraries (no 32-bit fallback) cannot run
        // inside this build; the clone screen explains that case explicitly instead of a black screen.
        ndk { abiFilters += listOf("armeabi-v7a") }
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
