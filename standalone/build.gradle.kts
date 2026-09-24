plugins {
    kotlin("jvm")
    alias(libs.plugins.shadow)
    alias(libs.plugins.fabric.loom)
}

val buildHash = try {
    val command = arrayOf("git", "rev-parse", "HEAD")
    val process = Runtime.getRuntime().exec(command)
    process.inputReader().readLine().trim()
} catch (e: Throwable) {
    e.printStackTrace()
    "<unknown>"
}

setupCommonUnmodded("standalone", javaVersion = 25)

allprojects {
    apply(plugin = "java")
    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin = "com.gradleup.shadow")

    if (project.name == "launcher")
        setupCommonUnmodded("launcher", javaVersion = if (System.getenv("GITHUB_RUN_NUMBER") == null) 25 else 17)

    dependencies {
        val libs = rootProject.project.libs
        shadedDep(implementation(project(":api"))!!)
        shadedDep(implementation(project(":shared"))!!)

        implementation(libs.bundles.kotlin)
        shadedDep(libs.bundles.kotlin)

        shadedDep(implementation(libs.datafixerupper.get())!!)

        implementation(libs.bundles.logging)
        shadedDep(libs.bundles.logging)
    }
}

dependencies {
    val shadedDep by configurations.getting

    minecraft("com.mojang:minecraft:${libs.versions.minecraft.standalone.get()}")

    shadedDep(implementation(project(":common:26.3")) {
        isTransitive = false
    })

    shadedDep(implementation(project(":transcribers:google"))!!)
    shadedDep(implementation(project(":transcribers:whisper"))!!)
}

tasks {
    processResources {
        properties(listOf("metadata.json"),
            "version" to mod.version,
            "minecraft_version" to libs.versions.minecraft.standalone.get(),
            "build_time" to System.currentTimeMillis(),
            "build_hash" to buildHash
        )
    }
}
