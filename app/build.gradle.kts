import java.util.Date
import java.text.SimpleDateFormat

val currentDate = Date()
val sdf = SimpleDateFormat("yyyyMMdd")

plugins {
    id("com.android.application")
    kotlin("android")
}

android {
    namespace = "tw.lospot.kin.wirelesshid"
    compileSdk = 33
    defaultConfig {
        applicationId = "tw.lospot.kin.wirelesshid"
        minSdk = 28
        targetSdk = 33
        versionCode = 1
        versionName = "1.0_${sdf.format(currentDate)}"
    }
    buildTypes {
        getByName("release") {
            isShrinkResources = true
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = compileOptions.targetCompatibility.toString()
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.4.7"
    }
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib")
    implementation("org.jetbrains.kotlin:kotlin-reflect")

    implementation("androidx.core:core-ktx:1.10.1")
    implementation("androidx.navigation:navigation-compose:2.5.3")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.6.1")
    implementation("androidx.activity:activity-compose:1.7.1")
    implementation("androidx.datastore:datastore-preferences:1.0.0")

    val composeBom = platform("androidx.compose:compose-bom:2023.05.01")
    implementation(composeBom)
    androidTestImplementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material:material")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.foundation:foundation-layout")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
}
