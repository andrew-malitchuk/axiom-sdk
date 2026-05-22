plugins {
    `kotlin-dsl`
    `java-gradle-plugin`
}

group = "convention"

java {
    val javaVersion = JavaVersion.toVersion(libs.versions.java.get())
    sourceCompatibility = javaVersion
    targetCompatibility = javaVersion
}

kotlin {
    jvmToolchain(libs.versions.java.get().toInt())
}

dependencies {
    compileOnly(libs.android.gradle.plugin)
    compileOnly(libs.kotlin.gradle.plugin)
    implementation(libs.detekt.gradle.plugin)
    implementation(libs.ktlint.gradle.plugin)
    implementation(libs.compose.compiler.gradle)
}

gradlePlugin {
    plugins {
        register("androidLibrary") {
            id = "convention.library"
            implementationClass = "convention.source.conventionplugin.android.AndroidLibraryConventionPlugin"
        }
        register("androidApplication") {
            id = "convention.application"
            implementationClass = "convention.source.conventionplugin.android.AndroidApplicationConventionPlugin"
        }
        register("codeQuality") {
            id = "convention.quality"
            implementationClass = "convention.source.conventionplugin.core.CodeQualityConventionPlugin"
        }
        register("androidPublish") {
            id = "convention.publish"
            implementationClass = "convention.source.conventionplugin.android.AndroidPublishConventionPlugin"
        }
    }
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    compilerOptions {
        apiVersion.set(org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_2_1)
        languageVersion.set(org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_2_1)
    }
}
