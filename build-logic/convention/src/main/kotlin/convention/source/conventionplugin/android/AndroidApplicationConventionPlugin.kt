package convention.source.conventionplugin.android

import convention.core.ext.app
import convention.core.ext.configureAndroidBase
import convention.core.ext.configureKotlinBase
import convention.core.ext.getVersionAsInt
import convention.core.ext.getVersionAsString
import convention.core.ext.implementDependency
import convention.core.ext.libs
import convention.core.ext.plugins
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

public class AndroidApplicationConventionPlugin : Plugin<Project> {

    override fun apply(target: Project): Unit = with(target) {
        plugins {
            apply("com.android.application")
            apply("org.jetbrains.kotlin.plugin.compose")
            apply("convention.quality")
        }

        configureKotlinBase()

        app {
            configureAndroidBase(this)

            buildFeatures {
                compose = true
            }

            defaultConfig {
                applicationId = getVersionAsString("applicationId")
                targetSdk = getVersionAsInt("targetSdk")
                versionCode = getVersionAsInt("versionCode")
                versionName = getVersionAsString("versionName")
            }
        }

        dependencies {
            val bom = libs.findLibrary("compose-bom").get()
            add("implementation", platform(bom))

            implementDependency(versionCatalog = libs, value = "core.ktx")
            implementDependency(versionCatalog = libs, value = "material3")
            implementDependency(versionCatalog = libs, value = "ui.tooling")
            implementDependency(versionCatalog = libs, value = "ui.tooling.preview")
            implementDependency(versionCatalog = libs, value = "activity.compose")
            implementDependency(versionCatalog = libs, value = "compose.runtime")
        }
    }
}
