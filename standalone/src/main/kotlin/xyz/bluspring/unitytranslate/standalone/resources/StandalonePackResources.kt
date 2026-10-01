package xyz.bluspring.unitytranslate.standalone.resources

import net.minecraft.resources.Identifier
import net.minecraft.server.packs.PackLocationInfo
import net.minecraft.server.packs.PackResources
import net.minecraft.server.packs.PackType
import net.minecraft.server.packs.metadata.MetadataSectionType
import net.minecraft.server.packs.resources.IoSupplier
import xyz.bluspring.unitytranslate.UnityTranslate
import xyz.bluspring.unitytranslate.standalone.UnityTranslateStandalone
import java.io.InputStream

object StandalonePackResources : PackResources {
    override fun getRootResource(vararg path: String): IoSupplier<InputStream>? {
        val stream = UnityTranslateStandalone::class.java.getResourceAsStream("/${path.joinToString("/")}") ?: return null
        return IoSupplier { stream }
    }

    override fun getResource(type: PackType, location: Identifier): IoSupplier<InputStream>? {
        val stream = UnityTranslateStandalone::class.java.getResourceAsStream("/${type.directory}/${location.namespace}/${location.path}") ?: return null
        return IoSupplier { stream }
    }

    override fun listResources(type: PackType, namespace: String, directory: String, output: PackResources.ResourceOutput) {
        TODO("Not yet implemented")
    }

    override fun getNamespaces(type: PackType): Set<String> {
        return setOf(Identifier.DEFAULT_NAMESPACE, UnityTranslate.MOD_ID)
    }

    override fun <T : Any> getMetadataSection(metadataSerializer: MetadataSectionType<T>): T? {
        TODO("Not yet implemented")
    }

    override fun location(): PackLocationInfo {
        TODO("Not yet implemented")
    }

    override fun close() {
    }
}
