plugins {
    alias(libs.plugins.moddevgradle.legacy)
    alias(libs.plugins.shadow)
}

val mcVersion = stonecutter.current.version
val common = stonecutter.node.sibling("")!!

legacyForge {
    version = "$mcVersion-${property("forge")}"

    configureModDev(this, "forge")
}

mixin {
    config("${mod.id}.mixins.json")
    config("${mod.id}.forge.mixins.json")
}

setupCommon("forge")
setupCommonModDev("forge")

val shadedDep by configurations.named("shadedDep")

dependencies {
    api(project(":shared")) {
        isTransitive = false
    }

    api(libs.mixinextras.forge)
    annotationProcessor(libs.mixinextras.forge)
    jarJar(libs.mixinextras.forge)
    moddedApi("dev.nyon:KotlinLangForge:${libs.versions.kotlinlangforge.get()}-${klfLangVersion}+forge")
}
