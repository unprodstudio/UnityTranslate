package xyz.bluspring.unitytranslate.client.renderer.ui.awt.shape

import java.awt.geom.Path2D

@Suppress("RemoveRedundantQualifierName") // Blame Java's Path2D.Double
data class RRShape(val width: kotlin.Double, val height: kotlin.Double, val radius: kotlin.Double) : Path2D.Float() {
    constructor(width: kotlin.Float, height: kotlin.Float, radius: kotlin.Float) : this(width.toDouble(), height.toDouble(), radius.toDouble())

    init {
        moveTo(0.0, 0.0)
        lineTo(width - radius, 0.0)
        curveTo(width, 0.0, width, 0.0, width, radius)
        lineTo(width, height - radius)
        curveTo(width, height, width, height, width - radius, height)
        lineTo(0.0, height)
        closePath()
    }
}
