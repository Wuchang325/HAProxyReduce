pluginManagement {
    val kotlinVersion: String = "2.2.0"

    plugins {
        kotlin("jvm") version kotlinVersion
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}
rootProject.name = "HAProxyReduce"

include(":common")
include(":velocity")
include(":paper")
