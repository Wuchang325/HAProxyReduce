plugins {
    kotlin("jvm")
    id("com.gradleup.shadow") version "9.6.0"
}

val nettyVersion: String = findProperty("nettyVersion") as String? ?: "4.1.79.Final"
val paperApiVersion: String = findProperty("paperApiVersion") as String? ?: "1.20.1-R0.1-SNAPSHOT"
val spigotApiVersion: String = findProperty("spigotApiVersion") as String? ?: "1.20.1-R0.1-SNAPSHOT"
val protocolLibVersion: String = findProperty("protocolLibVersion") as String? ?: "5.4.0"
val bstatsBukkitVersion: String = findProperty("bstatsBukkitVersion") as String? ?: "3.0.2"

dependencies {
    // 依赖Common模块
    implementation(project(":common")) {
        //isTransitive = false
    }

    compileOnly("org.spigotmc:spigot-api:$spigotApiVersion")
    compileOnly("io.papermc.paper:paper-api:$paperApiVersion")
    compileOnly("net.dmulloy2:ProtocolLib:$protocolLibVersion")
    compileOnly("io.netty:netty-codec:$nettyVersion")
    compileOnly("io.netty:netty-codec-haproxy:$nettyVersion")
    implementation("org.bstats:bstats-bukkit:$bstatsBukkitVersion")
    implementation(kotlin("stdlib-jdk8"))
}

tasks {
    shadowJar {
        archiveBaseName.set("HAProxyReduce-Paper")
        archiveClassifier.set("")

        // 重定位避免冲突
        relocate("org.bstats", "top.zient.haproxyreduce.bukkit.bstats")
        relocate("top.zient.haproxyreduce.common", "top.zient.haproxyreduce.bukkit.common")

        mergeServiceFiles()
    }

    processResources {
        filesMatching("paper-plugin.yml") {
            expand(mapOf("version" to project.version))
        }
    }

    build {
        dependsOn(shadowJar)
    }
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/") // 包含Bukkit和Paper依赖
    maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
    maven("https://repo.dmulloy2.net/repository/public/")
}

kotlin {
    jvmToolchain(21)
}
