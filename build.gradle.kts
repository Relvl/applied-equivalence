plugins {
    kotlin("jvm") version "1.7.21"
    id("fabric-loom") version "1.0-SNAPSHOT"
}

group = "io.github.relvl"
version = "1.0.0-SNAPSHOT"


repositories {
    maven("https://modmaven.dev/")
    maven("https://maven.architectury.dev")
    maven("https://maven.shedaniel.me")
    maven("https://maven.bai.lol")
    maven("https://maven.parchmentmc.org")
}

val minecraft_version: String by project
val yarn_mappings: String by project
val loader_version: String by project
val fabric_version: String by project

dependencies {
    minecraft("com.mojang", "minecraft", minecraft_version)
    mappings(loom.layered {
        officialMojangMappings()
        parchment("org.parchmentmc.data:parchment-1.18.2:2022.11.06@zip")
    })

    modImplementation("net.fabricmc", "fabric-loader", loader_version)
    modImplementation("net.fabricmc.fabric-api", "fabric-api", fabric_version)
    modImplementation("net.fabricmc", "fabric-language-kotlin", "1.8.6+kotlin.1.7.21")

    modImplementation("teamreborn", "energy", "2.2.0") {
        exclude(group = "net.fabricmc.fabric-api")
    }

    modImplementation("appeng:appliedenergistics2-fabric:11.6.2") {
        isTransitive = false
    }

    modRuntimeOnly("dev.architectury:architectury-fabric:4.10.86") { exclude(group = "net.fabricmc.fabric-api") }
    modRuntimeOnly("me.shedaniel:RoughlyEnoughItems-fabric:8.3.571") { exclude(group = "net.fabricmc.fabric-api") }
    modRuntimeOnly("me.shedaniel.cloth:cloth-config-fabric:6.4.90") { exclude(group = "net.fabricmc.fabric-api") }
    modRuntimeOnly("mcp.mobius.waila:wthit:fabric-4.13.5") { exclude(group = "net.fabricmc.fabric-api") }
    compileOnly("mcp.mobius.waila:wthit-api:fabric-4.13.5") { exclude(group = "net.fabricmc.fabric-api") }
}

tasks {
    val javaVersion = JavaVersion.VERSION_17
    withType<JavaCompile> {
        options.encoding = "UTF-8"
        sourceCompatibility = javaVersion.toString()
        targetCompatibility = javaVersion.toString()
        options.release.set(javaVersion.toString().toInt())
    }
    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions { jvmTarget = javaVersion.toString() }
    }
    jar { from("LICENSE") { rename { "${it}_${base.archivesName}" } } }
    processResources {
        inputs.property("version", project.version)
        filesMatching("fabric.mod.json") { expand(mutableMapOf("version" to project.version)) }
    }
    java {
        toolchain { languageVersion.set(JavaLanguageVersion.of(javaVersion.toString())) }
        sourceCompatibility = javaVersion
        targetCompatibility = javaVersion
        withSourcesJar()
    }
}
