plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.locationlibrary"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.locationlibrary"
        minSdk = 26
        targetSdk = 34
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

//    implementation(project(":locationlibrary"))
    implementation("com.github.bendayaniv:LocationLibrary:1.00.00")

//    implementation("androidx.appcompat:appcompat:1.6.1")
//    implementation("com.google.android.gms:play-services-location:21.0.1")
}