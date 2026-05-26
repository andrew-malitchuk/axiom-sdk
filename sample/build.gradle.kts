plugins {
    id("convention.application")
    id("convention.quality")
}

android {
    namespace = "dev.axiom.sdk.sample"
}

dependencies {
    implementation(projects.sdk)
}
