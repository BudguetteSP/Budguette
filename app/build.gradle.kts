plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.gms.google-services")
    id("kotlin-kapt")
}

android {
    namespace = "com.example.budguette"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.budguette"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
        isCoreLibraryDesugaringEnabled = true
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }

    buildFeatures {
        viewBinding = true
    }
}

// Force androidx.core version to avoid duplicates
configurations.all {
    resolutionStrategy {
        force("androidx.core:core-ktx:1.15.0")
    }
}

dependencies {
    // AndroidX
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("androidx.activity:activity-ktx:1.9.0")
    implementation("androidx.constraintlayout:constraintlayout:2.2.0")
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")
    implementation("com.google.android.material:material:1.11.0")

    // Firebase
    implementation(platform("com.google.firebase:firebase-bom:33.12.0"))
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.firebase:firebase-firestore-ktx")
    implementation("com.google.firebase:firebase-storage-ktx")
    implementation("com.google.firebase:firebase-analytics-ktx")
    implementation("com.google.firebase:firebase-common-ktx:20.4.2")
    implementation("com.google.android.gms:play-services-auth:21.0.0")

    // Glide
    implementation("com.github.bumptech.glide:glide:4.16.0") {
        exclude(group = "com.android.support")
    }
    kapt("com.github.bumptech.glide:compiler:4.16.0")

    // MPAndroidChart
    implementation("com.github.PhilJay:MPAndroidChart:3.1.0") {
        exclude(group = "com.android.support")
    }

    // Facebook Login
    implementation("com.facebook.android:facebook-login:15.2.0") {
        exclude(group = "com.android.support")
    }

    // Material CalendarView
    implementation("com.prolificinteractive:material-calendarview:1.4.3") {
        exclude(group = "com.android.support")
    }

    // ThreeTenABP for date handling
    implementation("com.jakewharton.threetenabp:threetenabp:1.4.6")

    // Core library desugaring
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.4")

    // Charts, testing
    implementation("com.github.PhilJay:MPAndroidChart:3.1.0")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}
