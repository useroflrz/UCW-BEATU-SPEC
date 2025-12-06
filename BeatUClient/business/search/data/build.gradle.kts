plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.kapt)
    alias(libs.plugins.hilt)
}

android {
    namespace = "com.ucw.beatu.business.search.data"
    compileSdk = 36

    defaultConfig {
        minSdk = 24
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
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
}

dependencies {
    // Business Domain
    implementation(project(":business:search:domain"))
    implementation(project(":business:videofeed:domain"))
    
    // Business Data (for VideoFeedApiService)
    implementation(project(":business:videofeed:data"))
    
    // Shared modules
    implementation(project(":shared:common"))
    implementation(project(":shared:network"))
    implementation(project(":shared:database"))
    
    // Retrofit
    implementation(libs.retrofit)
    implementation(libs.retrofit.moshi)
    
    // Moshi (for direct JSON parsing in AISearchApiService)
    implementation(libs.moshi)
    implementation(libs.moshi.kotlin)
    
    // OkHttp (for SSE streaming)
    implementation(libs.okhttp)
    
    // Room
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    kapt(libs.androidx.room.compiler)
    
    // Coroutines
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)

    // Hilt
    implementation(libs.hilt.android)
    kapt(libs.hilt.compiler)
}

