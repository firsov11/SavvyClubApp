import com.android.build.gradle.internal.cxx.configure.gradleLocalProperties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.google.services)
}

android {
    namespace = "com.example.savvyclub"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.savvyclub"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        val props = gradleLocalProperties(rootDir, providers)
        val googleClientId = props.getProperty("GOOGLE_CLIENT_ID")
        buildConfigField("String", "GOOGLE_CLIENT_ID", "\"$googleClientId\"")
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
        buildConfig = true // <-- Включаем генерацию BuildConfig
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)


    implementation(libs.core.ktx)
    implementation(libs.activity.compose)
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)
    implementation(libs.navigation.compose)
    implementation(libs.gson)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.commons.compress)
    implementation(libs.okhttp)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.play.services.auth)
    implementation(libs.firebase.auth.ktx)
    implementation(libs.play.services.identity)

    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.playservices.auth)
    implementation(libs.google.identity)

    implementation(libs.credentials)
    implementation(libs.credentials.play.services.auth)
}