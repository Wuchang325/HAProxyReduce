plugins {
    kotlin("jvm")
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

dependencies {
    implementation(project(":common"))
    compileOnly("com.velocitypowered:velocity-api:3.4.0-SNAPSHOT")
    implementation("io.netty:netty-codec:4.1.79.Final")
    implementation("io.netty:netty-codec-haproxy:4.1.79.Final")
    implementation("org.bstats:bstats-velocity:3.0.2")

    annotationProcessor("com.velocitypowered:velocity-api:3.4.0-SNAPSHOT")
    implementation(kotlin("stdlib-jdk8"))
}

tasks {
    // 配置 Shadow JAR 输出
    shadowJar {
        archiveBaseName.set("HAProxyReduce-Velocity")
        archiveClassifier.set("") // 移除默认的 "all" 后缀
        from("velocity-plugin.json") // 包含插件元数据

        relocate("org.bstats", "top.zient.haproxyreduce.velocity.bstats")

        // 合并依赖到最终 JAR
        mergeServiceFiles()
    }

    // 让 build 任务自动执行 shadowJar
    build {
        dependsOn(shadowJar)
    }
}

repositories {
    mavenCentral()
    // 添加 Shadow 插件的仓库
    maven { url = uri("https://plugins.gradle.org/m2/") }
}

kotlin {
    jvmToolchain(17)
}
