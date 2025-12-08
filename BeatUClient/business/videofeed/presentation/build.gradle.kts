plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.kapt)
    alias(libs.plugins.hilt)
    id("kotlin-parcelize")
}

android {
    namespace = "com.ucw.beatu.business.videofeed.presentation"
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
    implementation(project(":business:videofeed:domain"))
    
    // User domain and data (for getting user data in VideoItemFragment)
    implementation(project(":business:user:domain"))
    implementation(project(":business:user:data"))  // 需要依赖 data 模块以获取 UserRepository 的 Hilt 绑定
    
    // Settings domain (for reading playback settings)
    implementation(project(":business:settings:domain"))
    
    // Shared modules
    implementation(project(":shared:common"))
    implementation(project(":shared:player"))
    implementation(project(":shared:designsystem"))
    implementation(project(":shared:router"))
    
    // AndroidX
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.fragment.ktx)
    implementation(libs.androidx.viewpager2)
    implementation(libs.androidx.paging.runtime.ktx)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    
    // ExoPlayer (for PlayerView in layout)
    // Note: exoplayer-core is needed for DataBinding to resolve AdViewProvider
    implementation(libs.exoplayer.core)
    implementation(libs.exoplayer.ui)
    
    // Hilt
    implementation(libs.hilt.android)
    kapt(libs.hilt.compiler)
    
    // Material
    implementation(libs.material)
    // Coil for image loading in ImagePostFragment
    implementation(libs.coil)

    // QRCode 生成（分享海报用）
    implementation("com.google.zxing:core:3.5.3")
}

