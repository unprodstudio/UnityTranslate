package xyz.bluspring.unitytranslate.api.v2.client.util

import xyz.bluspring.unitytranslate.api.v2.UnityTranslateApi
import kotlin.math.pow
import kotlin.math.sin

enum class CommonEasing(private val easing: Easing) : Easing {
    LINEAR(Easing { x -> x }),

    SMOOTH(Easing { x -> x * x * (3f - 2f * x) }),

    // Bounce
    BOUNCE_OUT(Easing { t ->
        var t = t
        val n1 = 7.5625f
        val d1 = 2.75f

        if (t < 1f / d1) return@Easing n1 * t * t

        if (t < 2f / d1) {
            t -= 1.5f / d1
            return@Easing n1 * t * t + 0.75f
        }

        if (t < 2.5f / d1) {
            t -= 2.25f / d1
            return@Easing n1 * t * t + 0.9375f
        }

        t -= 2.625f / d1
        n1 * t * t + 0.984375f
    }),
    BOUNCE_IN(Easing { t -> 1f - BOUNCE_OUT.getValue(1f - t) }),
    BOUNCE_IN_OUT(Easing { x ->
        if (x < 0.5f)
            (1f - BOUNCE_OUT.getValue(1f - 2f * x)) / 2f
        else
            (1f + BOUNCE_OUT.getValue(2f * x - 1f)) / 2f
    }
    ),

    // Elastic
    ELASTIC_OUT(Easing { t ->
        if (t == 0f) return@Easing 0f
        if (t == 1f) return@Easing 1f
        val c4 = (2f * Math.PI.toFloat()) / 3f

        (2.0f.pow((-10f * t)) * sin(((t * 10f - 0.75f) * c4)) + 1f)
    }),
    ELASTIC_IN(Easing { t ->
        if (t == 0f) return@Easing 0f
        if (t == 1f) return@Easing 1f

        val p = 0.3f
        (-2.0f).pow((10f * (t - 1f))) * sin((t - 1f - p / 4f) * (2f * Math.PI) / p).toFloat()
    }),
    ELASTIC_IN_OUT(Easing { t ->
        if (t == 0f) return@Easing 0f
        if (t == 1f) return@Easing 1f
        val p = 0.45f
        val v = sin((20f * t - 11.125f) * (2 * Math.PI) / p).toFloat()

        if (t < 0.5f)
            return@Easing -0.5f * 2.0f.pow((20 * t - 10)) * v

        2f.pow((-20f * t + 10f)) * v * 0.5f + 1f
    }),

    // Back
    BACK_IN(Easing { x -> 2.70158f * x * x * x - 1.70158f * x * x }),
    BACK_OUT(Easing { x ->
        val u: Float = 1f - x
        1f - BACK_IN.getValue(u)
    }),
    BACK_IN_OUT(Easing { x ->
        val c1 = 1.70158f
        val c2 = c1 * 1.525f
        if (x < 0.5) return@Easing ((2f * x).pow(2.0f) * ((2f * c2 + 1f) * 2f * x - 2f * c2)) / 2f
        ((2f * x - 2f).pow(2.0f) * ((2f * c2 + 1f) * (x * 2f - 2f) + 2f * c2) + 2f) / 2f
    }),

    /** Polynomials */ // Quadratic
    EASE_IN_QUADRATIC(Easing.polynomialEaseIn(2)),
    EASE_OUT_QUADRATIC(Easing.polynomialEaseOut(2)),
    EASE_IN_OUT_QUADRATIC(Easing.polynomialEaseInOut(2)),

    // Cubic
    EASE_IN_CUBIC(Easing.polynomialEaseIn(3)),
    EASE_OUT_CUBIC(Easing.polynomialEaseOut(3)),
    EASE_IN_OUT_CUBIC(Easing.polynomialEaseInOut(3)),

    // Quartic
    EASE_IN_QUARTIC(Easing.polynomialEaseIn(4)),
    EASE_OUT_QUARTIC(Easing.polynomialEaseOut(4)),
    EASE_IN_OUT_QUARTIC(Easing.polynomialEaseInOut(4)),

    // Quintic
    EASE_IN_QUINTIC(Easing.polynomialEaseIn(5)),
    EASE_OUT_QUINTIC(Easing.polynomialEaseOut(5)),
    EASE_IN_OUT_QUINTIC(Easing.polynomialEaseInOut(5)),
    ;

    override fun getValue(t: Float): Float {
        if (!UnityTranslateApi.instance.client.shouldAnimate)
            return 1f

        return this.getValueUnsafe(t)
    }

    /**
     * A version of [getValue] that calls directly rather than handling a check that ensures for disabled animations.
     */
    fun getValueUnsafe(t: Float): Float = this.easing.getValue(t)
}
