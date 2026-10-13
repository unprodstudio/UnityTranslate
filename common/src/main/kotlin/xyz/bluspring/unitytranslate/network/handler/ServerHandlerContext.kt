package xyz.bluspring.unitytranslate.network.handler

import java.util.*

@JvmRecord
data class ServerHandlerContext(
    val source: UUID,
    val serverProxy: ServerProxy,
)
