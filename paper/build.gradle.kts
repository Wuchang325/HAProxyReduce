plugins {
    kotlin("jvm")
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

dependencies {
    // 依赖Common模块
    implementation(project(":common")) {
        //isTransitive = false
    }

    compileOnly("org.spigotmc:spigot-api:1.20.1-R0.1-SNAPSHOT")
    compileOnly("io.papermc.paper:paper-api:1.20.1-R0.1-SNAPSHOT")
    compileOnly("com.comphenix.protocol:ProtocolLib:5.1.0")
    implementation("io.netty:netty-codec:4.1.79.Final")
    implementation("io.netty:netty-codec-haproxy:4.1.79.Final")
    implementation("org.bstats:bstats-bukkit:3.0.2")
    implementation(kotlin("stdlib-jdk8"))
}

tasks {
    shadowJar {
        archiveBaseName.set("HAProxyReduce-Paper")
        archiveClassifier.set("")
        from("src/main/resources")

        // 重定位避免冲突
        relocate("org.bstats", "top.zient.haproxyreduce.bukkit.bstats")
        relocate("top.zient.haproxyreduce.common", "top.zient.haproxyreduce.bukkit.common")

        mergeServiceFiles()
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
    jvmToolchain(17)
}