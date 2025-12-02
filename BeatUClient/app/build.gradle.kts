plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.kapt)
    alias(libs.plugins.hilt)
}

android {
    namespace = "com.ucw.beatu"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.ucw.beatu"
        minSdk = 24
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
        viewBinding = true
    }
}

dependencies {
    // Shared modules
    implementation(project(":shared:common"))
    implementation(project(":shared:network"))
    implementation(project(":shared:database"))
    implementation(project(":shared:player"))
    implementation(project(":shared:designsystem"))
    
    // Business modules - Presentation layers
    implementation(project(":business:videofeed:presentation"))
    implementation(project(":business:videofeed:data"))
    implementation(project(":business:user:presentation"))
    implementation(project(":business:user:data"))
    implementation(project(":business:user:domain"))
    implementation(project(":business:search:presentation"))
    implementation(project(":business:ai:presentation"))
    implementation(project(":business:landscape:presentation"))
    implementation(project(":business:settings:presentation"))
    implementation(project(":business:landscape:data"))
    implementation(project(":business:settings:data"))
    
    // Fragment
    implementation(libs.androidx.fragment.ktx)
    
    // AndroidX Core
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.fragment.ktx)
    implementation(libs.material)
    implementation(libs.androidx.constraintlayout)
    
    // Lifecycle
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    
    // Navigation
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    
    // Hilt
    implementation(libs.hilt.android)
    kapt(libs.hilt.compiler)
    implementation(libs.hilt.navigation.fragment)
    
    // Coroutines
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)
    
    // Test
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    // OkHttp
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    // Retrofit
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    // Moshi
    implementation("com.squareup.moshi:moshi:1.15.0")
    implementation("com.squareup.retrofit2:converter-moshi:2.9.0")


        // Room
        implementation("androidx.room:room-runtime:2.6.1")
        kapt("androidx.room:room-compiler:2.6.1")

        // 如果使用 Kotlin，还需要这个
        implementation("androidx.room:room-ktx:2.6.1")

}