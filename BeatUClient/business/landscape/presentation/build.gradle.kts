plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.kapt)
    alias(libs.plugins.hilt)
    id("kotlin-parcelize")
}

android {
    namespace = "com.ucw.beatu.business.landscape.presentation"
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
    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    // Business Domain
    implementation(project(":business:landscape:domain"))
    
    // Business Data
    implementation(project(":business:landscape:data"))
    
    // Business Presentation (for VideoItem model)
    implementation(project(":business:videofeed:presentation"))
    
    // Shared modules
    implementation(project(":shared:common"))
    implementation(project(":shared:player"))  // 播放器
    implementation(project(":shared:designsystem"))
    
    // AndroidX
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.fragment.ktx)
    implementation(libs.androidx.viewpager2)  // ViewPager2
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    
    // ExoPlayer
    implementation(libs.exoplayer.core)
    implementation(libs.exoplayer.ui)
    
    // Hilt
    implementation(libs.hilt.android)
    kapt(libs.hilt.compiler)
    
    // Material
    implementation(libs.material)
}

