plugins {
    id("convention.library")
    id("convention.quality")
    id("convention.publish")
}

group = "io.github.andrew-malitchuk"
version = "0.0.1"

android {
    namespace = "dev.axiom.sdk"
}

dependencies {
    testImplementation(libs.junit4)

    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.window.testing)
    androidTestImplementation(libs.activity.compose)
    debugImplementation(libs.compose.ui.test.manifest)
}
