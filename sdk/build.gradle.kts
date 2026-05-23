plugins {
    id("convention.library")
    id("convention.quality")
    id("convention.publish")
}

group = "dev.axiom"
version = "1.0.0"

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
