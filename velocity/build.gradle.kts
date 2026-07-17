plugins {
   kotlin("jvm")
   id("com.gradleup.shadow") version "9.6.0"
}

val nettyVersion: String = findProperty("nettyVersion") as String? ?: "4.1.79.Final"
val snakeYamlVersion: String = findProperty("snakeyamlVersion") as String? ?: "2.2"
val bstatsVelocityVersion: String = findProperty("bstatsVelocityVersion") as String? ?: "3.0.2"
val releaseBuild: Boolean = findProperty("releaseBuild")?.toString()?.toBoolean() ?: true
val velocityApiVersion: String = if (releaseBuild) "3.3.0-SNAPSHOT" else "3.5.0-SNAPSHOT"

repositories {
   mavenCentral()
   maven("https://repo.papermc.io/repository/maven-public/")
   maven("https://plugins.gradle.org/m2/")
}

dependencies {
   implementation(project(":common"))
   compileOnly("com.velocitypowered:velocity-api:$velocityApiVersion")
   compileOnly("io.netty:netty-codec:$nettyVersion")
   compileOnly("io.netty:netty-codec-haproxy:$nettyVersion")
   implementation("org.yaml:snakeyaml:$snakeYamlVersion")
   implementation("org.bstats:bstats-velocity:$bstatsVelocityVersion")
   implementation(kotlin("stdlib"))
}

kotlin {
   jvmToolchain(21)
   compilerOptions {
       freeCompilerArgs.add("-Xannotation-default-target=param-property")
   }
}

tasks.processResources {
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
    filesMatching("velocity-plugin.json") {
        filter { line ->
            line.replace("\${project.version}", rootProject.version.toString())
                .replace("\${project.name}", project.name.toString())
        }
    }
}

sourceSets.main {
   resources {
       srcDir(layout.buildDirectory.dir("generated/resources/main"))
   }
}

tasks.shadowJar {
   archiveBaseName.set("HAProxyReduce-Velocity")
   archiveVersion.set(rootProject.version.toString())

   relocate("org.yaml.snakeyaml", "top.zient.haproxyreduce.libs.snakeyaml")
   relocate("org.bstats", "top.zient.haproxyreduce.velocity.bstats")
   mergeServiceFiles()
}

tasks.build {
   dependsOn(tasks.shadowJar)
}
