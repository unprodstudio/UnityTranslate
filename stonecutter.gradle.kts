import dev.kikugie.fletching_table.extension.FletchingTableExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinProjectExtension

plugins {
    id("dev.kikugie.stonecutter")
    alias(libs.plugins.fabric.loom) apply false
    alias(libs.plugins.moddevgradle) version libs.versions.moddevgradle.get() apply false
    alias(libs.plugins.kotlin) apply false
    alias(libs.plugins.mod.publish) apply false
    alias(libs.plugins.shadow) apply false
    alias(libs.plugins.fletching.table) apply false
    id("idea")
    id("multiplatform")
}

stonecutter active "26.3"

allprojects {
    apply(plugin = "idea")

    repositories {
        fun mavenProviding(url: String, vararg groups: String) {
            exclusiveContent {
                forRepositories(maven(url)).filter {
                    for (group in groups) {
                        includeGroupAndSubgroups(group)
                    }
                }
            }
        }

        mavenCentral()
        mavenLocal()
        mavenProviding("https://maven.parchmentmc.org", "org.parchmentmc.data")
        mavenProviding("https://repo.nyon.dev/releases", "dev.nyon")
        mavenProviding("https://maven.fabricmc.net", "net.fabricmc")
        mavenProviding("https://libraries.minecraft.net", "com.mojang")
        mavenProviding("https://mvn.devos.one/releases", "xyz.bluspring.sunset", "xyz.bluspring.modernnetworking", "io.nayuki")
        mavenProviding("https://maven.maxhenkel.de/releases", "de.maxhenkel")
        mavenProviding("https://api.modrinth.com/maven", "maven.modrinth")
        mavenProviding("https://www.cursemaven.com", "curse.maven")
        mavenProviding("https://repo.plasmoverse.com/releases", "su.plo.voice", "su.plo.slib")
    }

    group = mod.group
    version = mod.version

    // IDEA no longer automatically downloads sources/javadoc jars for dependencies, so we need to explicitly enable the behavior.
    idea {
        module {
            isDownloadSources = true
            isDownloadJavadoc = true
        }
    }
}

subprojects {
    if (project.extensions.findByName("stonecutter") == null)
        return@subprojects

    //if (parent == rootProject)
        //return@subprojects

    apply(plugin = "java")
    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin = "maven-publish")
    apply(plugin = "dev.kikugie.fletching-table")

    project.extensions.configure<FletchingTableExtension>("fletchingTable") {
        j52j.register("main") {
            extension("json", "*.mixins.json5", "*.mod.json5")
        }
    }

    project.extensions.configure<JavaPluginExtension>("java") {
        withSourcesJar()
        withJavadocJar()

        val java = JavaVersion.toVersion(project.minimumJavaVersion)
        targetCompatibility = java
        sourceCompatibility = java
    }

    project.extensions.configure<KotlinProjectExtension>("kotlin") {
        jvmToolchain(project.minimumJavaVersion)
    }

    tasks.named<Jar>("jar") {
        archiveClassifier = "dev"
    }
}

// Runs active versions for each loader
for (it in stonecutter.tree.nodes) {
    if (it.metadata != stonecutter.current || it.branch.id.isEmpty()) continue
    val types = listOf("Client", "Server")
    val loader = it.branch.id.upperCaseFirst()
    for (type in types) it.project.tasks.register("runActive$type$loader") {
        group = "project"
        dependsOn("run$type")
    }
}
