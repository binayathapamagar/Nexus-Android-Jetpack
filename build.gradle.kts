buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
    }
}

plugins {
    id("com.android.application") version "8.6.1" apply false
    id("com.android.library") version "8.6.1" apply false
    id("org.jetbrains.kotlin.android") version "1.9.22" apply false // Updated Kotlin version
    id("com.google.gms.google-services") version "4.4.2" apply false
}

// Add this to address deprecation warnings
tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
}