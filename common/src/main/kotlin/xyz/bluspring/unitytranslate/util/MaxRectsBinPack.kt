package xyz.bluspring.unitytranslate.util

import xyz.bluspring.unitytranslate.api.v2.client.util.ScreenRectangle
import kotlin.math.absoluteValue

/*
 	Based on the Public Domain MaxRectsBinPack.cpp source by Jukka Jylänki
 	https://github.com/juj/RectangleBinPack/

 	Ported to C# by Sven Magnus
 	https://github.com/jderrough/UnitySlippyMap/blob/master/Assets/UnitySlippyMap/Helpers/MaxRectsBinPack.cs

    Then ported by BluSpring for Kotlin usage, with some code reduction to make it not too painful to port.
    This version is also public domain - do whatever you want with it.
*/
class MaxRectsBinPack(width: Int, height: Int) {
    var width: Int = width
        private set

    var height: Int = height
        private set

    val usedRectangles: List<ScreenRectangle>
        field = mutableListOf()
    val freeRectangles: List<ScreenRectangle>
        field = mutableListOf()

    enum class FreeRectChoiceHeuristic {
        RectBestShortSideFit, //< -BSSF: Positions the rectangle against the short side of a free rectangle into which it fits the best.
        RectBestLongSideFit, //< -BLSF: Positions the rectangle against the long side of a free rectangle into which it fits the best.
        RectBestAreaFit, //< -BAF: Positions the rectangle into the smallest free rect into which it fits.
        RectBottomLeftRule, //< -BL: Does the Tetris placement.
    }

    fun reset(width: Int, height: Int) {
        this.width = width
        this.height = height

        this.usedRectangles.clear()

        val rect = ScreenRectangle(0, 0, width, height)
        this.freeRectangles.clear()
        this.freeRectangles.add(rect)
    }

    init {
        this.reset(width, height)
    }

    fun insert(width: Int, height: Int, method: FreeRectChoiceHeuristic = FreeRectChoiceHeuristic.RectBestAreaFit): ScreenRectangle {
        val score = Score()
        val newNode = when (method) {
            FreeRectChoiceHeuristic.RectBestShortSideFit -> findPosBSSF(width, height, score)
            FreeRectChoiceHeuristic.RectBestLongSideFit -> findPosBLSF(width, height, score)
            FreeRectChoiceHeuristic.RectBestAreaFit -> findPosBAF(width, height, score)
            FreeRectChoiceHeuristic.RectBottomLeftRule -> findPosBL(width, height, score)
        }

        if (newNode.height == 0)
            return newNode

        return this.place(newNode)
    }

    fun insert(rects: List<ScreenRectangle>, dest: MutableList<ScreenRectangle>, method: FreeRectChoiceHeuristic = FreeRectChoiceHeuristic.RectBestAreaFit) {
        dest.clear()

        val copy = rects.toMutableList()
        while (copy.isNotEmpty()) {
            var bestFirst = Int.MAX_VALUE
            var bestSecond = Int.MAX_VALUE
            var bestIndex = -1
            var bestNode = ScreenRectangle(0, 0, 0, 0)

            for ((i, rect) in copy.withIndex()) {
                val score = Score()
                val newNode = scoreRect(rect.width, rect.height, method, score)
                if (score.first < bestFirst || (score.first == bestFirst && score.second < bestSecond)) {
                    bestFirst = score.first
                    bestSecond = score.second
                    bestNode = newNode
                    bestIndex = i
                }
            }

            if (bestIndex == -1)
                return

            place(bestNode)
            copy.removeAt(bestIndex)
        }
    }

    fun remove(rect: ScreenRectangle) {
        usedRectangles.remove(rect)
        freeRectangles.add(rect)
        pruneFreeList()
    }

    private fun place(node: ScreenRectangle): ScreenRectangle {
        var currentNode = node
        var size = freeRectangles.size
        var i = 0

        while (i < size) {
            if (splitFreeNode(freeRectangles[i], currentNode)) {
                freeRectangles.removeAt(i)
                --i
                --size
            }

            i++
        }

        pruneFreeList()
        usedRectangles.add(currentNode)
        return currentNode
    }

    private fun scoreRect(width: Int, height: Int, method: FreeRectChoiceHeuristic, score: Score): ScreenRectangle {
        score.first = Int.MAX_VALUE
        score.second = Int.MAX_VALUE

        val newNode = when (method) {
            FreeRectChoiceHeuristic.RectBestShortSideFit -> findPosBSSF(width, height, score)
            FreeRectChoiceHeuristic.RectBestLongSideFit -> findPosBLSF(width, height, score)
            FreeRectChoiceHeuristic.RectBestAreaFit -> findPosBAF(width, height, score)
            FreeRectChoiceHeuristic.RectBottomLeftRule -> findPosBL(width, height, score)
        }

        if (newNode.height == 0) {
            score.first = Int.MAX_VALUE
            score.second = Int.MAX_VALUE
        }

        return newNode
    }

    val occupancy: Double
        get() {
            var usedSurfaceArea = 0L
            for (rect in usedRectangles) {
                usedSurfaceArea += rect.width.toLong() * rect.height.toLong()
            }

            return (usedSurfaceArea.toDouble() / (width * height).toDouble())
        }

    private fun findPosBL(width: Int, height: Int, score: Score): ScreenRectangle {
        var bestNode = ScreenRectangle(0, 0, 0, 0)
        score.first = Int.MAX_VALUE

        for (rect in freeRectangles) {
            // Try to place the rectangle in upright (non-flipped) orientation.
            if (rect.width >= width && rect.height >= height) {
                val topSideY = rect.y + height
                if (topSideY < score.first || (topSideY == score.first && rect.x < score.second)) {
                    bestNode = ScreenRectangle(rect.x, rect.y, width, height)
                    score.first = topSideY
                    score.second = rect.x
                }
            }
        }

        return bestNode
    }

    private fun findPosBSSF(width: Int, height: Int, score: Score): ScreenRectangle {
        var bestNode = ScreenRectangle(0, 0, 0, 0)
        score.first = Int.MAX_VALUE

        for (rect in freeRectangles) {
            // Try to place the rectangle in upright (non-flipped) orientation.
            if (rect.width >= width && rect.height >= height) {
                val leftoverX = (rect.width - width).absoluteValue
                val leftoverY = (rect.height - height).absoluteValue
                val ssf = leftoverX.coerceAtMost(leftoverY)
                val lsf = leftoverX.coerceAtLeast(leftoverY)

                if (ssf < score.first || (ssf == score.first && lsf < score.second)) {
                    bestNode = ScreenRectangle(rect.x, rect.y, height, width)
                    score.first = ssf
                    score.second = lsf
                }
            }
        }

        return bestNode
    }

    private fun findPosBLSF(width: Int, height: Int, score: Score): ScreenRectangle {
        var bestNode = ScreenRectangle(0, 0, 0, 0)
        score.second = Int.MAX_VALUE

        for (rect in freeRectangles) {
            // Try to place the rectangle in upright (non-flipped) orientation.
            if (rect.width >= width && rect.height >= height) {
                val leftoverX = (rect.width - width).absoluteValue
                val leftoverY = (rect.height - height).absoluteValue
                val ssf = leftoverX.coerceAtMost(leftoverY)
                val lsf = leftoverX.coerceAtLeast(leftoverY)

                if (ssf < score.second || (ssf == score.second && lsf < score.first)) {
                    bestNode = ScreenRectangle(rect.x, rect.y, height, width)
                    score.first = ssf
                    score.second = lsf
                }
            }
        }

        return bestNode
    }

    private fun findPosBAF(width: Int, height: Int, score: Score): ScreenRectangle {
        var bestNode = ScreenRectangle(0, 0, 0, 0)
        score.first = Int.MAX_VALUE

        for (rect in freeRectangles) {
            val areaFit = rect.width * rect.height - width * height

            // Try to place the rectangle in upright (non-flipped) orientation.
            if (rect.width >= width && rect.height >= height) {
                val leftoverX = (rect.width - width).absoluteValue
                val leftoverY = (rect.height - height).absoluteValue
                val ssf = leftoverX.coerceAtMost(leftoverY)

                if (areaFit < score.first || (areaFit == score.first && ssf < score.second)) {
                    bestNode = ScreenRectangle(rect.x, rect.y, height, width)
                    score.first = areaFit
                    score.second = ssf
                }
            }
        }

        return bestNode
    }

    private fun splitFreeNode(free: ScreenRectangle, used: ScreenRectangle): Boolean {
        // Test with SAT if the rects even intersect
        if (used.x >= free.x + free.width || used.x + used.width <= free.x || used.y >= free.y + free.height || used.y + used.height <= free.y)
            return false

        if (used.x < free.x + free.width && used.x + used.width > free.x) {
            // New node at the top side of the used node.
            if (used.y > free.y && used.y < free.y + free.height) {
                val newNode = ScreenRectangle(free.x, free.y, free.width, used.y - free.y)
                freeRectangles.add(newNode)
            }

            // New node at the bottom side of the used node.
            if (used.y + used.height < free.y + free.height) {
                val newNode = ScreenRectangle(free.x, used.y + used.height, free.width, free.y + free.height - (used.y + used.height))
                freeRectangles.add(newNode)
            }
        }

        if (used.y < free.y + free.height && used.y + used.height > free.y) {
            // New node at the left side of the used node.
            if (used.x > free.x && used.x < free.x + free.width) {
                val newNode = ScreenRectangle(free.x, free.y, used.x - free.x, free.height)
                freeRectangles.add(newNode)
            }

            // New node at the right side of the used node.
            if (used.x + used.width < free.x + free.width) {
                val newNode = ScreenRectangle(used.x + used.width, free.y, free.x + free.width - (used.x + used.width), free.height)
                freeRectangles.add(newNode)
            }
        }

        return true
    }

    private fun pruneFreeList() {
        var i = 0

        while (i < freeRectangles.size) {
            var j = i + 1

            while (j < freeRectangles.size) {
                if (freeRectangles[i].contains(freeRectangles[j])) {
                    freeRectangles.removeAt(i)
                    --i
                    break
                }

                if (freeRectangles[j].contains(freeRectangles[i])) {
                    freeRectangles.removeAt(j)
                    --j
                }

                ++j
            }

            ++i
        }
    }

    private data class Score(var first: Int = 0, var second: Int = 0)
}
