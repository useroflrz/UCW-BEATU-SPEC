// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.kapt) apply false
    alias(libs.plugins.hilt) apply false
}

subprojects {
    configurations.configureEach {
        // prism4j-bundler pulls annotations-java5, which conflicts with org.jetbrains:annotations.
        exclude(group = "org.jetbrains", module = "annotations-java5")
    }
}