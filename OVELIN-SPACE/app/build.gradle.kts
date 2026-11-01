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
        versionCode = 4
        versionName = "0.2.2"
    }

    // One APK per CPU bit-width. The virtual runtime executes the guest inside this app's own
    // process, so the host must have the same bit-width as the app being cloned: install
    // app-armeabi-v7a for 32-bit apps (e.g. WhatsApp on many phones), app-arm64-v8a for 64-bit apps.
    splits {
        abi {
            isEnable = true
            reset()
            include("armeabi-v7a", "arm64-v8a")
            isUniversalApk = false
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
