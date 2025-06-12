import java.net.URL

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.example.sampleapplication"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.sampleapplication"
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
    }
}

// Download aar files from GitHub
val sdkVersion = "v0.0.1"
task("downloadAarFiles") {
    doLast {
        println("Download AARs task started...")
        val aarUrl =
            "https://github.com/microsoft/MultimodalClientSDK.Android/releases/download/$sdkVersion/MultimodalClientSDK.aar"
        val aarFile = file("${project.rootDir}/app/libs/MultimodalClientSDK.aar")
        URL(aarUrl).openStream()
            .use { input -> aarFile.outputStream().use { output -> input.copyTo(output) } }
    }
}

tasks.named("preBuild") {
    dependsOn("downloadAarFiles")
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

    // MultimodalClientSDK dependency
    val ktorVersion = "2.3.2"
    implementation(mapOf("name" to "MultimodalClientSDK", "ext" to "aar"))
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("io.ktor:ktor-client-okhttp:$ktorVersion")
    implementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")
    implementation("com.microsoft.cognitiveservices.speech:client-sdk:1.41.1")
    implementation("com.google.code.gson:gson:2.8.9")
    implementation("io.adaptivecards:adaptivecards-android:3.6.1")

}