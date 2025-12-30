import java.util.Date
import java.text.SimpleDateFormat

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
    id("kotlin-parcelize")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("com.google.dagger.hilt.android")
}

import java.util.Properties
import java.io.FileInputStream

// ... (existing code)

val properties = Properties()
val localPropertiesFile = project.rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    properties.load(FileInputStream(localPropertiesFile))
}

dependencies {
    // ... (existing dependencies)
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")
    implementation("androidx.work:work-runtime-ktx:2.9.0")

    
    // Core
    implementation("androidx.core:core-ktx:1.12.0")
    // ...
}

android {
    namespace = "com.curbos.pos"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.curbos.pos"
        minSdk = 26
        targetSdk = 34
        versionCode = 13
        versionName = "0.2.3-experimental-dev"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
        manifestPlaceholders["auth0Domain"] = "dev-7myakdl4itf644km.us.auth0.com"
        manifestPlaceholders["auth0Scheme"] = "demo"
        // Enterprise Security: Read secrets from environment (CI) or local.properties (dev)
        val supabaseUrl = System.getenv("SUPABASE_URL") ?: properties.getProperty("SUPABASE_URL") ?: ""
        val supabaseKey = System.getenv("SUPABASE_KEY") ?: properties.getProperty("SUPABASE_KEY") ?: ""

        buildConfigField("String", "SUPABASE_URL", "\"$supabaseUrl\"")
        buildConfigField("String", "SUPABASE_KEY", "\"$supabaseKey\"")
    }

    buildFeatures {
        compose = true
        buildConfig = true // Enable BuildConfig generation
    }

    signingConfigs {
        create("release") {
            // Priority: Environment Variables (CI/CD) -> local.properties (Dev)
            val keystoreFile = if (System.getenv("CI") == "true") {
                 file("keystore.jks") // In CI, we decode to root of app dir
            } else {
                 file("../release.jks") // Local dev expectation
            }
            
            if (keystoreFile.exists()) {
                storeFile = keystoreFile
                storePassword = System.getenv("SIGNING_STORE_PASSWORD") ?: properties.getProperty("storePassword")
                keyAlias = System.getenv("SIGNING_KEY_ALIAS") ?: properties.getProperty("keyAlias")
                keyPassword = System.getenv("SIGNING_KEY_PASSWORD") ?: properties.getProperty("keyPassword")
            } else {
                 println("Release keystore not found at ${keystoreFile.absolutePath}, skipping signing config.")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("release")
        }
        debug {
            val date = SimpleDateFormat("yyyyMMdd-HH").format(Date())
            versionNameSuffix = "-nightly-$date"
            applicationIdSuffix = ".debug"
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "META-INF/DEPENDENCIES"
            excludes += "META-INF/LICENSE"
            excludes += "META-INF/LICENSE.txt"
            excludes += "META-INF/license.txt"
            excludes += "META-INF/NOTICE"
            excludes += "META-INF/NOTICE.txt"
            excludes += "META-INF/notice.txt"
            excludes += "META-INF/ASL2.0"
            excludes += "META-INF/*.kotlin_module" 
        }
    }
}

dependencies {
    // Core
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.2")
    implementation("androidx.activity:activity-compose:1.8.2")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.6.2")

    // Compose
    implementation(platform("androidx.compose:compose-bom:2024.06.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material3:material3-window-size-class") // Adaptive UI
    implementation("androidx.compose.ui:ui-text-google-fonts:1.5.4") // Google Fonts
    
    // AppCompat (Required for BiometricPrompt)
    implementation("androidx.appcompat:appcompat:1.6.1")
    
    // Icons
    implementation("androidx.compose.material:material-icons-extended")

    // Navigation
    implementation("androidx.navigation:navigation-compose:2.7.5")

    // Room
    val roomVersion = "2.6.1"
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    ksp("androidx.room:room-compiler:$roomVersion")
    implementation("com.auth0.android:auth0:2.10.2")

    // Supabase
    implementation("io.github.jan-tennert.supabase:postgrest-kt:2.0.0")
    implementation("io.github.jan-tennert.supabase:realtime-kt:2.0.0")
    implementation("io.github.jan-tennert.supabase:gotrue-kt:2.0.0")
    implementation("io.ktor:ktor-client-android:2.3.6")
    implementation("io.ktor:ktor-client-cio:2.3.6")

    // Square (Refactored to use raw Intents to avoid dependency issues)
    // implementation("com.squareup.sdk.pos:pos-sdk:3.5.0")

    // Retrofit (Xero Integration)
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    
    // Biometrics
    implementation("androidx.biometric:biometric:1.2.0-alpha05")

    // Coil (Image Loading)
    implementation("io.coil-kt:coil-compose:2.5.0")

    // Testing
    testImplementation("junit:junit:4.13.2")
    testImplementation("io.mockk:mockk:1.13.8")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    testImplementation("androidx.arch.core:core-testing:2.2.0")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    androidTestImplementation("androidx.test:rules:1.6.1")
    androidTestImplementation("androidx.test:runner:1.6.1")
    androidTestImplementation("androidx.test.uiautomator:uiautomator:2.3.0")
    androidTestImplementation(platform("androidx.compose:compose-bom:2024.06.00"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    // Hilt
    implementation("com.google.dagger:hilt-android:2.48")
    ksp("com.google.dagger:hilt-android-compiler:2.48")
    implementation("androidx.hilt:hilt-navigation-compose:1.1.0")
    implementation("androidx.hilt:hilt-work:1.1.0")
    ksp("androidx.hilt:hilt-compiler:1.1.0")

    // P2P / Nearby Connections
    implementation("com.google.android.gms:play-services-nearby:19.0.0")

    // QR Code Generation
    implementation("com.google.zxing:core:3.5.3")
}
