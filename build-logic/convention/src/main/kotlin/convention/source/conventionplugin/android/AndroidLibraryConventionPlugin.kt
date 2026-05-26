package convention.source.conventionplugin.android

import convention.core.ext.configureAndroidBase
import convention.core.ext.configureKotlinBase
import convention.core.ext.implementDependency
import convention.core.ext.lib
import convention.core.ext.libs
import convention.core.ext.plugins
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

public class AndroidLibraryConventionPlugin : Plugin<Project> {

    override fun apply(target: Project): Unit = with(target) {
        plugins {
            apply("com.android.library")
            apply("org.jetbrains.kotlin.plugin.compose")
            apply("convention.quality")
        }

        configureKotlinBase()

        lib {
            configureAndroidBase(this)

            buildFeatures {
                compose = true
            }
        }

        dependencies {
            val bom = libs.findLibrary("compose-bom").get()
            add("implementation", platform(bom))

            implementDependency(versionCatalog = libs, value = "kotlinx.coroutines.core")
            implementDependency(versionCatalog = libs, value = "kotlinx.coroutines.android")
            implementDependency(versionCatalog = libs, value = "compose.runtime")
            implementDependency(versionCatalog = libs, value = "compose.foundation")
            implementDependency(versionCatalog = libs, value = "ui")
            implementDependency(versionCatalog = libs, value = "ui.tooling.preview")
            implementDependency(versionCatalog = libs, value = "androidx.window")
        }
    }
}
