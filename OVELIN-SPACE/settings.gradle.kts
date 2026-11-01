pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // FreeReflection (hidden-API access used by the virtualization engine) is only published here.
        maven {
            url = uri("https://jitpack.io")
            content { includeGroup("com.github.tiann") }
        }
    }
}

rootProject.name = "OVELIN-SPACE"
include(":app")
include(":Bcore")
include(":black-reflection")
include(":compiler")
