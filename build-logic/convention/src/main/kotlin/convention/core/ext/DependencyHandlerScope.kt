package convention.core.ext

import org.gradle.api.artifacts.VersionCatalog
import org.gradle.kotlin.dsl.DependencyHandlerScope

fun DependencyHandlerScope.implementDependency(versionCatalog: VersionCatalog, value: String) {
    add("implementation", versionCatalog.findLibrary(value).get())
}
