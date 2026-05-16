plugins {
    kotlin("jvm")
    id("com.gradleup.shadow") version "8.3.5"
}

val nettyVersion: String = findProperty("nettyVersion") as String? ?: "4.1.79.Final"
val snakeYamlVersion: String = findProperty("snakeyamlVersion") as String? ?: "2.2"
val bstatsVelocityVersion: String = findProperty("bstatsVelocityVersion") as String? ?: "3.0.2"
val releaseBuild: Boolean = findProperty("releaseBuild")?.toString()?.toBoolean() ?: true
val velocityApiVersion: String = if (releaseBuild) "3.3.0" else "3.5.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://plugins.gradle.org/m2/")
    maven {
        url = uri("https://repo.velocitypowered.com/releases/")
        content {
            includeGroup("com.velocitypowered")
        }
    }
    maven {
        url = uri("https://repo.velocitypowered.com/snapshots/")
        content {
            includeGroup("com.velocitypowered")
        }
    }
}

dependencies {
    implementation(project(":common"))
    compileOnly("com.velocitypowered:velocity-api:$velocityApiVersion")
    implementation("io.netty:netty-codec:$nettyVersion")
    implementation("io.netty:netty-codec-haproxy:$nettyVersion")
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

// 资源处理任务
tasks.register<Copy>("processPluginJson") {
    group = "build"
    description = "处理 velocity-plugin.json 中的版本变量"

    from("src/main/resources") {
        include("velocity-plugin.json")
        filter { line ->
            line.replace("\${project.version}", rootProject.version.toString())
                .replace("\${project.name}", project.name.toString())
        }
    }
    into(layout.buildDirectory.dir("generated/resources/main"))
}

tasks.processResources {
    dependsOn("processPluginJson")
    exclude("velocity-plugin.json")
}

sourceSets.main {
    resources {
        srcDir(layout.buildDirectory.dir("generated/resources/main"))
    }
}

// Shadow JAR 配置
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
