pluginManagement {
    repositories {
        maven("https://maven.fabricmc.net/")
        maven("https://maven.minecraftforge.net/")
        maven("https://maven.neoforged.net/releases/")
        maven("https://maven.kikugie.dev/releases")
        maven("https://maven.kikugie.dev/snapshots")
        gradlePluginPortal()
    }
}

// can't use libs.versions.toml for this - https://github.com/gradle/gradle/issues/36437
// make sure to update it there too tho.
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0" // https://plugins.gradle.org/plugin/org.gradle.toolchains.foojay-resolver-convention
    id("dev.kikugie.stonecutter") version "0.9.6" // https://stonecutter.kikugie.dev/
}

val supportedVersions = listOf(
//    "1.21.1", "1.21.4", "1.21.5", "1.21.8", "1.21.10", "1.21.11", "26.1.2", "26.2",// TODO: we'll work backwards
    "26.3"
)

stonecutter {
    centralScript = "build.gradle.kts"
    kotlinController = true

    create(rootProject) {
        versions(supportedVersions)
        vcsVersion = "26.3"

        branch("common")
        branch("fabric")
//        branch("neoforge") {
//            // NeoForge doesn't exist for <=1.20.1
//            versions(supportedVersions.filter {
//                stonecutter.eval(it, ">1.20.1") &&
//                    stonecutter.eval(it, "<=26.1") // Neo doesn't support snapshots
//            })
//        }
        // IMPORTANT NOTE TO SELF. "Settings are not yet ready for build" ARE BECAUSE THERE EXISTS AN EMPTY VERSIONS LIST SOMEWHERE.
//        branch("forge") {
//            // KLF doesn't exist for >=1.20.5, don't bother
//            versions(supportedVersions.filter { stonecutter.eval(it, "<1.20.5") })
//        }
    }
}

include(":api")
include(":shared")
//include(":bukkit") // TODO: enough separation for Bukkit
include(":standalone", ":standalone:launcher")
include(":transcribers", ":transcribers:google", ":transcribers:whisper")
include(":relay")

includeBuild("build-logic")

rootProject.name = "UnityTranslate"
