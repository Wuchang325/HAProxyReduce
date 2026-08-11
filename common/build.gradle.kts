plugins {
    kotlin("jvm")
}

val nettyVersion: String = findProperty("nettyVersion") as String? ?: "4.1.79.Final"
val snakeYamlVersion: String = findProperty("snakeyamlVersion") as String? ?: "2.2"
val bstatsBaseVersion: String = findProperty("bstatsBaseVersion") as String? ?: "3.0.0"
val slf4jVersion: String = findProperty("slf4jVersion") as String? ?: "2.0.9"

repositories {
    mavenCentral()
}

dependencies {
    compileOnly("org.yaml:snakeyaml:$snakeYamlVersion")
    compileOnly("io.netty:netty-codec:$nettyVersion")
    compileOnly("io.netty:netty-codec-haproxy:$nettyVersion")
    compileOnly("commons-validator:commons-validator:1.7")
    compileOnly("org.bstats:bstats-base:$bstatsBaseVersion")
    compileOnly("org.slf4j:slf4j-api:$slf4jVersion")
    compileOnly(kotlin("stdlib-jdk8"))
}

kotlin {
    jvmToolchain(21)
}
