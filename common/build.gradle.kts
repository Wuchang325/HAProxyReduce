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
    implementation("org.yaml:snakeyaml:$snakeYamlVersion")
    implementation("io.netty:netty-codec:$nettyVersion")
    implementation("io.netty:netty-codec-haproxy:$nettyVersion")
    implementation("commons-validator:commons-validator:1.7")
    implementation("org.bstats:bstats-base:$bstatsBaseVersion")
    implementation("org.slf4j:slf4j-api:$slf4jVersion")
    implementation(kotlin("stdlib-jdk8"))
}

kotlin {
    jvmToolchain(21)
}
