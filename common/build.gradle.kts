plugins {
    kotlin("jvm")
}
dependencies {
    implementation("io.netty:netty-codec:4.1.79.Final")
    implementation("io.netty:netty-codec-haproxy:4.1.79.Final")
    implementation("commons-validator:commons-validator:1.7")
    implementation("org.bstats:bstats-base:3.0.0")
    implementation(kotlin("stdlib-jdk8"))
}
repositories {
    mavenCentral()
}
kotlin {
    jvmToolchain(17)
}