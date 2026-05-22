package convention.source.conventionplugin.android

import convention.core.ext.lib
import convention.core.ext.plugins
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.the
import org.gradle.plugins.signing.SigningExtension

/**
 * Convention plugin that wires up Maven publication and GPG signing for an Android library.
 *
 * ## Publication
 * Registers a single `release` publication backed by AGP's `release` component (AAR +
 * transitive metadata). Sources JAR is included via `singleVariant("release")`.
 * The module sets [Project.group] and [Project.version] in its own `build.gradle.kts`;
 * this plugin picks them up automatically.
 *
 * ## Repositories
 * - `mavenLocal()` — always present, useful for local consumer testing.
 * - Sonatype OSSRH — release versions go to the staging repo; SNAPSHOT versions go to
 *   the snapshot repo. Credentials are read from env vars or `gradle.properties`.
 *
 * ## Signing
 * Signs the `release` publication using GPG. Signing is **required for release versions**
 * and **optional for SNAPSHOTs** so local and CI snapshot builds do not fail when no key
 * is configured.
 *
 * Key resolution order (both local and CI-friendly):
 * 1. Environment variables: `SIGNING_KEY_ID`, `SIGNING_KEY`, `SIGNING_KEY_PASSWORD`
 * 2. Project-local file: `configure/signing/secrets.properties` (gitignored)
 * 3. Gradle properties (e.g. `~/.gradle/gradle.properties`):
 *    `signing.keyId`, `signing.key`, `signing.password`
 *
 * `signing.key` must be the ASCII-armored private key as a single line with literal `\n`
 * separators (the output of `gpg --armor --export-secret-keys <KEY_ID>` collapsed to one line).
 * Store it in `configure/signing/secrets.properties` for local development or as a CI secret
 * for automated builds. Never commit key material to the repo.
 *
 * OSSRH credential resolution order:
 * 1. Environment variables: `OSSRH_USERNAME`, `OSSRH_PASSWORD`
 * 2. Project-local file: `configure/signing/secrets.properties`
 * 3. Gradle properties: `ossrhUsername`, `ossrhPassword`
 */
public class AndroidPublishConventionPlugin : Plugin<Project> {

    override fun apply(target: Project): Unit = with(target) {
        plugins {
            apply("maven-publish")
            apply("signing")
        }

        lib {
            publishing {
                singleVariant("release") {
                    withSourcesJar()
                }
            }
        }

        afterEvaluate {
            val isSnapshot = version.toString().endsWith("SNAPSHOT")

            val secretsFile = rootProject.file("configure/signing/secrets.properties")
            val secrets = java.util.Properties().also { props ->
                if (secretsFile.exists()) secretsFile.reader().use { props.load(it) }
            }

            configure<PublishingExtension> {
                publications {
                    register<MavenPublication>("release") {
                        from(project.components.getByName("release"))

                        pom {
                            name.set("Axiom SDK")
                            description.set(
                                "Hinge angle sensor as Kotlin Flow and Compose State, " +
                                    "plus fold posture detection and adaptive layout utilities " +
                                    "for foldable Android devices.",
                            )
                            url.set("https://github.com/andrew-malitchuk/axiom-sdk")

                            licenses {
                                license {
                                    name.set("Apache License, Version 2.0")
                                    url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                                    distribution.set("repo")
                                }
                            }

                            developers {
                                developer {
                                    id.set("andrew-malitchuk")
                                    name.set("Andrew Malitchuk")
                                    email.set("andrew.malitchuk@gmail.com")
                                }
                            }

                            scm {
                                connection.set("scm:git:github.com/andrew-malitchuk/axiom-sdk.git")
                                developerConnection.set("scm:git:ssh://github.com/andrew-malitchuk/axiom-sdk.git")
                                url.set("https://github.com/andrew-malitchuk/axiom-sdk/tree/main")
                            }
                        }
                    }
                }

                repositories {
                    mavenLocal()
                    maven {
                        name = "sonatype"
                        url = if (isSnapshot) {
                            uri("https://s01.oss.sonatype.org/content/repositories/snapshots/")
                        } else {
                            uri("https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/")
                        }
                        credentials {
                            username = System.getenv("OSSRH_USERNAME")
                                ?: secrets.getProperty("ossrhUsername")
                                ?: findProperty("ossrhUsername")?.toString()
                            password = System.getenv("OSSRH_PASSWORD")
                                ?: secrets.getProperty("ossrhPassword")
                                ?: findProperty("ossrhPassword")?.toString()
                        }
                    }
                }
            }

            configure<SigningExtension> {
                val keyId = System.getenv("SIGNING_KEY_ID")
                    ?: secrets.getProperty("signing.keyId")
                    ?: findProperty("signing.keyId")?.toString()
                val key = System.getenv("SIGNING_KEY")
                    ?: secrets.getProperty("signing.key")
                    ?: findProperty("signing.key")?.toString()
                val password = System.getenv("SIGNING_KEY_PASSWORD")
                    ?: secrets.getProperty("signing.password")
                    ?: findProperty("signing.password")?.toString()

                if (keyId != null && key != null && password != null) {
                    useInMemoryPgpKeys(keyId, key, password)
                }

                // Required for releases; optional for snapshots so local builds do not
                // fail when no key is configured.
                setRequired { !isSnapshot }
                sign(the<PublishingExtension>().publications.getByName("release"))
            }
        }
    }
}
