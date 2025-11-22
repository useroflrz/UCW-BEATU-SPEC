plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.kapt)
}

android {
    namespace = "com.ucw.beatu.shared.common"
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
    // AndroidX Core
    implementation(libs.androidx.core.ktx)
    
    // Navigation (for NavigationHelper)
    // NavController is in navigation-runtime, which is included in navigation-fragment-ktx
    // We use the fragment version to avoid adding unnecessary dependencies
    implementation(libs.androidx.navigation.fragment.ktx)
    
    // Coroutines
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)
    
    // Moshi (for API response annotations)
    implementation(libs.moshi)
    implementation(libs.moshi.kotlin)
    kapt(libs.moshi.kotlin.codegen)
}

