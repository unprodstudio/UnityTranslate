package xyz.bluspring.unitytranslate.api.v2.client

import kotlin.math.floor

/**
 * Some utilities and constants that should be used when samples are passed to UnityTranslate.
 */
object AudioHelper {
    // Signed float32, little-endian, mono audio

    const val SAMPLE_RATE = 16_000
    const val SAMPLE_SIZE = 16
    const val CHANNELS = 1
    const val FRAME_SIZE = 2
    const val BUFFER_SIZE = (SAMPLE_RATE / 1000) * 20

    @JvmStatic
    fun ByteArray.toAudioFloatArray(): FloatArray {
        val floats = FloatArray(this.size / 2)

        for (i in 0 until this.size / 2) {
            if ((this[i * 2 + 1].toInt() and 0x80) != 0) {
                floats[i] = (Short.MIN_VALUE + ((this[i * 2 + 1].toInt() and 0x7F) shl 8) or (this[i * 2].toInt() and 0xFF)).toFloat()
            } else {
                floats[i] = (((this[i * 2 + 1].toInt() shl 8) and 0xFF00) or (this[i * 2].toInt() and 0xFF)).toFloat()
            }
        }

        return floats
    }

    @JvmStatic
    fun getMonoAudio(input: FloatArray, channels: Int): FloatArray {
        if (channels == CHANNELS)
            return input

        val outputLength = input.size / channels
        val output = FloatArray(outputLength)

        for (i in 0 until outputLength) {
            output[i] = input[i * channels]
        }

        return output
    }

    @JvmStatic
    fun downsampleAnyTo16k(input: FloatArray, sampleRate: Int): FloatArray {
        if (sampleRate == SAMPLE_RATE)
            return input

        val ratio = sampleRate.toFloat() / SAMPLE_RATE.toFloat()
        val outputLength = floor(input.size.toFloat() / ratio).toInt()
        val output = FloatArray(outputLength)

        for (i in 0 until outputLength) {
            val baseF = i.toFloat() * ratio
            val base = floor(baseF).toInt()
            val weight = baseF - base

            if (base + 1 < input.size) {
                output[i] = input[base] + weight * (input[base + 1] - input[base])
            } else {
                output[i] = input[base]
            }
        }

        return output
    }

    @JvmStatic
    fun downsample48kTo16k(input: FloatArray): FloatArray {
        val outputLength = input.size / 3
        val output = FloatArray(outputLength)

        for (i in 0 until outputLength) {
            val base = i * 3
            val sum = input[base] + input[base + 1] + input[base + 2]
            output[i] = sum / 3f
        }

        return output
    }
}
