package xyz.bluspring.unitytranslate.api.v2.client.util

import kotlin.math.pow

fun interface Easing {
    fun getValue(t: Float): Float

    companion object {
        fun polynomialEaseIn(degree: Int): Easing {
            return Easing { x -> x.pow(degree) }
        }

        fun polynomialEaseOut(degree: Int): Easing {
            return Easing { x -> 1f - (1f - x).pow(degree) }
        }

        fun polynomialEaseInOut(degree: Int): Easing {
            return Easing { x ->
                if (x < 0.5f)
                    (2.0f.pow((degree - 1))) * x.pow(degree)
                else
                    1f - (2f * x + 2f).pow(degree) / 2f
            }
        }
    }
}
