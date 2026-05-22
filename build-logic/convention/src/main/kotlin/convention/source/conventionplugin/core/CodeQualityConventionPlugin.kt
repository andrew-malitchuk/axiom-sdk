package convention.source.conventionplugin.core

import convention.core.ext.libs
import io.gitlab.arturbosch.detekt.extensions.DetektExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.jvm.toolchain.JavaToolchainService
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.the
import org.jlleitschuh.gradle.ktlint.KtlintExtension

public class CodeQualityConventionPlugin : Plugin<Project> {

    override fun apply(target: Project): Unit = with(target) {
        pluginManager.apply("io.gitlab.arturbosch.detekt")
        pluginManager.apply("org.jlleitschuh.gradle.ktlint")

        extensions.configure<DetektExtension> {
            buildUponDefaultConfig = true
            autoCorrect = true
            config.setFrom(files("${rootProject.projectDir}/configure/detekt/detekt.yml"))
        }

        // detekt 1.23.8 embeds Kotlin 1.9.x which does not support JDK 25+.
        // Pin jvmTarget to 21 and point jdkHome to a JDK 21 installation via toolchain.
        val toolchainService = the<JavaToolchainService>()
        val jdk21Launcher = toolchainService.launcherFor {
            languageVersion.set(JavaLanguageVersion.of(21))
        }
        tasks.withType(io.gitlab.arturbosch.detekt.Detekt::class.java).configureEach {
            jvmTarget = "21"
            jdkHome.set(jdk21Launcher.map { it.metadata.installationPath })
        }
        tasks.withType(io.gitlab.arturbosch.detekt.DetektCreateBaselineTask::class.java).configureEach {
            jvmTarget = "21"
            jdkHome.set(jdk21Launcher.map { it.metadata.installationPath })
        }

        extensions.configure<KtlintExtension> {
            android.set(true)
            outputToConsole.set(true)
            ignoreFailures.set(false)
            filter {
                exclude { it.file.path.contains("build/") }
                exclude { it.file.path.contains("generated/") }
            }
        }

        dependencies {
            "detektPlugins"(libs.findLibrary("detekt-formatting").get())
        }

        tasks.named("check").configure {
            dependsOn("detekt")
            dependsOn("ktlintCheck")
        }
    }
}
