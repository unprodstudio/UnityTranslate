import dev.kikugie.fletching_table.extension.FletchingTableExtension.Companion.relocate
import egt.RelocationTransform.Companion.registerRelocationAttribute
import net.fabricmc.loom.api.LoomGradleExtensionAPI

plugins {
    alias(libs.plugins.fletching.table)
    alias(libs.plugins.shadow)
    `maven-publish`
}

if (shouldRemap()) {
    apply(plugin = "net.fabricmc.fabric-loom-remap")
} else {
    apply(plugin = "net.fabricmc.fabric-loom")
}

setupCommon("fabric")
setupCommonLoom("fabric")

val loom = extensions.getByType<LoomGradleExtensionAPI>()

val shadedDep by configurations.named("shadedDep")
val common = stonecutter.node.sibling("")?.project

loom.accessWidenerPath = "fabric/src/main/resources/unitytranslate_${if (shouldRemap()) "obf" else "unobf"}.aw".run {
    rootProject.file(this)
}

val elementaConfig by configurations.creating {
    val relocated = registerRelocationAttribute("elementa-relocated") {
        relocate("gg.essential", "xyz.bluspring.unitytranslate.fork.elementa")
    }
    attributes { attribute(relocated, true) }
}

dependencies {
    implementation(project(":api"))
    runtimeOnly(project(":common:${stonecutter.current.version}"))
    runtimeOnly(project(":transcribers:whisper"))
    runtimeOnly(project(":transcribers:google"))

    moddedImplementation(libs.fabric.loader)
    moddedApi(libs.fabric.kotlin)
    api(libs.mixinextras.fabric)
    annotationProcessor(libs.mixinextras.fabric)
    moddedApi("net.fabricmc.fabric-api:fabric-api:${property("fabric_api")}")
    moddedApi(fletchingTable.modrinth("modmenu", stonecutter.current.version, "fabric"))

    shadedDep(runtimeOnly("xyz.bluspring.unitytranslate:unitytranslate-library:${libs.versions.unitytranslatelib.get()}:natives-windows-x64")!!)

    moddedApi("xyz.bluspring.modernnetworking:modernnetworking-fabric:${libs.versions.modernnetworking.get()}+${property("modernnetworking_mc")}")
    moddedApi("maven.modrinth:talk-balloons:${libs.versions.talk.balloons.get()}+${stonecutter.current.version}-fabric")
    moddedRuntimeOnly(fletchingTable.modrinth("simple-voice-chat", stonecutter.current.version, "fabric"))
}

tasks.processResources {
    relocate("unitytranslate_${if (shouldRemap()) "obf" else "unobf"}.aw") {
        this.name = "unitytranslate.aw"
    }
}
