plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.google.services) // <--- ADD THIS
}

android {
    namespace = "com.onrender.tutrnav"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "com.onrender.tutrnav"
        minSdk = 28
        targetSdk = 36
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
            signingConfig = signingConfigs.getByName("debug")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)



    // The "BoM" automatically picks the best versions for you
    implementation(platform("com.google.firebase:firebase-bom:33.9.0"))
    implementation("com.google.firebase:firebase-auth")

    // 4. Google Sign-In Library
    implementation("com.google.android.gms:play-services-auth:21.3.0")
    implementation("com.google.android.gms:play-services-maps:19.0.0")

    implementation("com.github.Dimezis:BlurView:version-3.2.0")

    // 1. Cloudinary (From your docs)
    implementation("com.cloudinary:cloudinary-android:3.0.2")

    // 2. Glide (For loading images)
    implementation("com.github.bumptech.glide:glide:4.16.0")

    // 3. Firebase Auth (Standard)
    implementation("com.google.firebase:firebase-auth:23.0.0")
    implementation("androidx.viewpager2:viewpager2:1.0.0")

    implementation("com.google.firebase:firebase-firestore:24.10.0")

    implementation("org.osmdroid:osmdroid-android:6.1.18")

    
}