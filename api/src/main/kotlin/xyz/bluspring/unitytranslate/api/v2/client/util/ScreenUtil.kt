package xyz.bluspring.unitytranslate.api.v2.client.util

import org.joml.Vector2f

object ScreenUtil {
    @JvmStatic
    fun findAnchorPoint(centerPos: Vector2f, width: Float, height: Float, screenWidth: Int, screenHeight: Int): Vector2f {
        return Vector2f(centerPos).div(screenWidth - (width / 2f), screenHeight - (height / 2f))
    }

    @JvmStatic
    fun ScreenRectangle.inflate(amount: Int): ScreenRectangle {
        return ScreenRectangle(this.left - amount, this.top - amount, this.width + amount + amount, this.height + amount + amount)
    }

    @JvmStatic
    fun union(vararg rectangles: ScreenRectangle): ScreenRectangle {
        if (rectangles.isEmpty())
            return ScreenRectangle.EMPTY

        val first = rectangles.first()
        var left = first.left
        var top = first.top
        var right = first.right
        var bottom = first.bottom

        for (rectangle in rectangles) {
            if (rectangle.left < left)
                left = rectangle.left

            if (rectangle.right > right)
                right = rectangle.right

            if (rectangle.top < top)
                top = rectangle.top

            if (rectangle.bottom > bottom)
                bottom = rectangle.bottom
        }

        return ScreenRectangle(left, top, right - left, bottom - top)
    }
}
