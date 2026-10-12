plugins {
    alias(libs.plugins.moddevgradle)
    alias(libs.plugins.shadow)
}

val mcVersion = stonecutter.current.version
val common = stonecutter.node.sibling("")!!

neoForge {
    version = property("neoforge") as String

    configureModDev(this, "neoforge")
}

setupCommon("neoforge")
setupCommonModDev("neoforge")

val shadedDep by configurations.named("shadedDep")

dependencies {
    api(project(":shared")) {
        isTransitive = false
    }

    api("dev.nyon:KotlinLangForge:${libs.versions.kotlinlangforge.get()}-${klfLangVersion}+neoforge")
}
