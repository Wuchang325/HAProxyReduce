plugins {
    kotlin("jvm")
}
allprojects {
    group = property("group").toString()
    version = property("version").toString()

    repositories {
        mavenCentral()
        maven("https://repo.papermc.io/repository/maven-public/")
        maven("https://plugins.gradle.org/m2/")
    }
}

subprojects {
    apply(plugin = "java")

    configure<JavaPluginExtension> {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(21))
        }
    }

    tasks.withType<JavaCompile> {
        options.encoding = "UTF-8"
    }
}
/*dependencies {
*    implementation(kotlin("stdlib-jdk8"))
}*/
