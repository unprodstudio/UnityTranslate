package xyz.bluspring.unitytranslate.standalone.launch

object FilteredClassLoader : ClassLoader() {
    init {
        registerAsParallelCapable()
    }

    override fun loadClass(name: String, resolve: Boolean): Class<*>? {
        if (name.startsWith("xyz.bluspring.unitytranslate.") || name.startsWith("xyz/bluspring/unitytranslate/")
            || name.startsWith("gg.essential.") || name.startsWith("gg/essential/")
        )
            throw ClassNotFoundException(name)

        return super.loadClass(name, resolve)
    }
}
