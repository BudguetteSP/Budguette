plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    id("com.google.gms.google-services") version "4.4.2" apply false
}

buildscript {
    val kotlin_version = "2.1.0"  // Update Kotlin version to match dependencies

    repositories {
        google()
        mavenCentral()
    }

    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlin_version")
        classpath("com.google.gms:google-services:4.4.2")  // Ensure Google services version is correct
    }
}
