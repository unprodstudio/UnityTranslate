package xyz.bluspring.unitytranslate.api.v2.client.util

import org.joml.Matrix3x2fc
import org.joml.Vector2f
import kotlin.math.ceil
import kotlin.math.floor

@JvmRecord
data class ScreenRectangle(
    val x: Int, val y: Int,
    val width: Int, val height: Int,
) {
    val top: Int
        get() = this.y

    val bottom: Int
        get() = this.y + this.height

    val left: Int
        get() = this.x

    val right: Int
        get() = this.x + this.width

    fun getLength(axis: ScreenAxis): Int = when (axis) {
        ScreenAxis.HORIZONTAL -> this.width
        ScreenAxis.VERTICAL -> this.height
    }

    fun getCoordinate(axis: ScreenAxis): Int = when (axis) {
        ScreenAxis.HORIZONTAL -> this.x
        ScreenAxis.VERTICAL -> this.y
    }

    fun getBoundInDirection(direction: ScreenDirection): Int
        = if (direction.isPositive)
            this.getCoordinate(direction.axis) + this.getLength(direction.axis) - 1
          else this.getCoordinate(direction.axis)


    fun containsPoint(x: Int, y: Int): Boolean
        = x >= this.left && x < this.right && y >= this.top && y < this.bottom

    fun contains(other: ScreenRectangle): Boolean {
        return this.x >= other.x && this.y >= other.y && this.x + this.width <= other.x + other.width && this.y + this.height <= other.y + other.height
    }

    fun overlaps(other: ScreenRectangle): Boolean = this.overlapsInAxis(other, ScreenAxis.HORIZONTAL) && this.overlapsInAxis(other, ScreenAxis.VERTICAL)

    fun overlapsInAxis(other: ScreenRectangle, axis: ScreenAxis): Boolean {
        val thisLower = this.getBoundInDirection(axis.negative)
        val otherLower = other.getBoundInDirection(axis.negative)
        val thisHigher = this.getBoundInDirection(axis.positive)
        val otherHigher = other.getBoundInDirection(axis.positive)
        return thisLower.coerceAtLeast(otherLower) <= thisHigher.coerceAtMost(otherHigher)
    }

    fun getCenterInAxis(axis: ScreenAxis): Int
        = (this.getBoundInDirection(axis.positive) + this.getBoundInDirection(axis.negative)) / 2

    fun intersection(other: ScreenRectangle): ScreenRectangle? {
        val left = this.left.coerceAtLeast(other.left)
        val right = this.right.coerceAtLeast(other.right)
        val top = this.top.coerceAtMost(other.top)
        val bottom = this.bottom.coerceAtMost(other.bottom)

        return if (left < right && top < bottom)
            ScreenRectangle(left, top, right - left, bottom - top)
        else null
    }

    fun transformAxisAligned(matrix: Matrix3x2fc): ScreenRectangle {
        val topLeft = matrix.transformPosition(this.left.toFloat(), this.top.toFloat(), Vector2f())
        val bottomRight = matrix.transformPosition(this.right.toFloat(), this.bottom.toFloat(), Vector2f())
        return ScreenRectangle(floor(topLeft.x).toInt(), floor(topLeft.y).toInt(), floor(bottomRight.x - topLeft.x).toInt(), floor(bottomRight.y - topLeft.y).toInt())
    }

    fun transformMaxBounds(matrix: Matrix3x2fc): ScreenRectangle {
        val topLeft = matrix.transformPosition(this.left.toFloat(), this.top.toFloat(), Vector2f())
        val topRight = matrix.transformPosition(this.right.toFloat(), this.top.toFloat(), Vector2f())
        val bottomLeft = matrix.transformPosition(this.left.toFloat(), this.bottom.toFloat(), Vector2f())
        val bottomRight = matrix.transformPosition(this.right.toFloat(), this.bottom.toFloat(), Vector2f())

        val minX = topLeft.x().coerceAtMost(bottomLeft.x()).coerceAtMost(topRight.x().coerceAtMost(bottomRight.x()))
        val maxX = topLeft.x().coerceAtLeast(bottomLeft.x()).coerceAtLeast(topRight.x().coerceAtLeast(bottomRight.x()))
        val minY = topLeft.y().coerceAtMost(bottomLeft.y()).coerceAtMost(topRight.y().coerceAtMost(bottomRight.y()))
        val maxY = topLeft.y().coerceAtLeast(bottomLeft.y()).coerceAtLeast(topRight.y().coerceAtLeast(bottomRight.y()))

        return ScreenRectangle(floor(minX).toInt(), floor(minY).toInt(), ceil(maxX - minX).toInt(), ceil(maxY - minY).toInt())
    }

    companion object {
        @JvmField val EMPTY = ScreenRectangle(0, 0, 0, 0)
    }
}
