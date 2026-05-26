package convention.source.conventionplugin.android

import convention.core.ext.lib
import convention.core.ext.plugins
import nmcp.NmcpExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.the
import org.gradle.plugins.signing.SigningExtension

/**
 * Convention plugin that wires up Maven publication, GPG signing, and Maven Central Portal
 * (via nmcp) for an Android library.
 *
 * ## Publication
 * Registers a single `release` publication backed by AGP's `release` component (AAR +
 * transitive metadata). Sources JAR is included via `singleVariant("release")`.
 * The module sets [Project.group] and [Project.version] in its own `build.gradle.kts`;
 * this plugin picks them up automatically.
 * Artifact ID is explicitly set to `axiom-sdk`.
 *
 * ## Repositories
 * `mavenLocal()` is always present for local consumer testing. Sonatype Central Portal
 * publication is handled by the nmcp plugin — run `./gradlew publishAllPublicationsToCentralPortal`.
 *
 * ## Signing
 * Signs the `release` publication using GPG. Required for release versions, optional for
 * SNAPSHOTs so local and CI snapshot builds succeed without a key configured.
 *
 * Key resolution order:
 * 1. Environment variables: `SIGNING_KEY_ID`, `SIGNING_KEY`, `SIGNING_KEY_PASSWORD`
 * 2. Project-local file: `configure/signing/secrets.properties` (gitignored)
 * 3. Gradle properties (`~/.gradle/gradle.properties`): `signing.keyId`, `signing.key`, `signing.password`
 *
 * `signing.key` must be the ASCII-armored private key as a single line with literal `\n`
 * separators. See `configure/signing/README.md` for details.
 *
 * Central Portal credential resolution order:
 * 1. Environment variables: `CENTRAL_USERNAME`, `CENTRAL_PASSWORD`
 * 2. Project-local file: `configure/signing/secrets.properties`
 * 3. Gradle properties: `centralUsername`, `centralPassword`
 */
public class AndroidPublishConventionPlugin : Plugin<Project> {

    override fun apply(target: Project): Unit = with(target) {
        plugins {
            apply("maven-publish")
            apply("signing")
            apply("com.gradleup.nmcp")
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

                        groupId = project.group.toString()
                        artifactId = "axiom-sdk"
                        version = project.version.toString()

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

            val centralUsername = System.getenv("CENTRAL_USERNAME")
                ?: secrets.getProperty("centralUsername")
                ?: findProperty("centralUsername")?.toString()
            val centralPassword = System.getenv("CENTRAL_PASSWORD")
                ?: secrets.getProperty("centralPassword")
                ?: findProperty("centralPassword")?.toString()

            configure<NmcpExtension> {
                publishAllPublications {
                    username.set(centralUsername ?: "")
                    password.set(centralPassword ?: "")
                    publicationType.set(if (isSnapshot) "AUTOMATIC" else "USER_MANAGED")
                }
            }
        }
    }
}
