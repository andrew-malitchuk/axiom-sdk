package convention.core.ext

import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.plugins.PluginManager
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.getByType
import org.jetbrains.kotlin.gradle.dsl.ExplicitApiMode
import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension

val Project.libs
    get(): VersionCatalog = extensions.getByType<VersionCatalogsExtension>().named("libs")

fun Project.getVersionAsString(name: String) = libs.findVersion(name).get().requiredVersion

fun Project.getVersionAsInt(name: String) = libs.findVersion(name).get().requiredVersion.toInt()

internal fun Project.configureAndroidBase(extension: ApplicationExtension) {
    extension.apply {
        compileSdk = getVersionAsInt("compileSdk")
        defaultConfig {
            minSdk = getVersionAsInt("minSdk")
        }
        compileOptions {
            val javaVersion = JavaVersion.toVersion(getVersionAsInt("java"))
            sourceCompatibility = javaVersion
            targetCompatibility = javaVersion
        }
    }
}

internal fun Project.configureAndroidBase(extension: LibraryExtension) {
    extension.apply {
        compileSdk = getVersionAsInt("compileSdk")
        defaultConfig {
            minSdk = getVersionAsInt("minSdk")
            testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        }
        compileOptions {
            val javaVersion = JavaVersion.toVersion(getVersionAsInt("java"))
            sourceCompatibility = javaVersion
            targetCompatibility = javaVersion
        }
    }
}

internal fun Project.configureKotlinBase() {
    val jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.fromTarget(getVersionAsString("java"))
    if (pluginManager.hasPlugin("com.android.application") ||
        pluginManager.hasPlugin("com.android.library")
    ) {
        extensions.configure<KotlinAndroidProjectExtension> {
            explicitApi = ExplicitApiMode.Strict
            compilerOptions {
                this.jvmTarget.set(jvmTarget)
            }
        }
    }
}

@Suppress("unused")
fun Project.app(block: ApplicationExtension.() -> Unit) {
    extensions.configure<ApplicationExtension> { block() }
}

@Suppress("unused")
fun Project.lib(block: LibraryExtension.() -> Unit) {
    extensions.configure<LibraryExtension> { block() }
}

@Suppress("unused")
fun Project.plugins(block: PluginManager.() -> Unit) {
    pluginManager.block()
}
