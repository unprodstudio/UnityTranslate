package xyz.bluspring.unitytranslate.client.transcriber.source

import xyz.bluspring.unitytranslate.api.v2.client.AudioHelper
import xyz.bluspring.unitytranslate.api.v2.transcriber.TranscriberSource
import xyz.bluspring.unitytranslate.api.v2.util.FloatRingBuffer
import xyz.bluspring.unitytranslate.capture.application.ApplicationAudioCapture
import xyz.bluspring.unitytranslate.capture.application.source.SourceInfo
import java.nio.ByteOrder

class ApplicationInputDeviceSource(private val sourceInfo: SourceInfo.Application, source: TranscriberSource) : InputSource(source) {
    private val buffer = FloatRingBuffer(2 * 1024 * 1024)
//    val tempBuffer = ByteBuffer.allocate(4 * 1024 * 1024)

    private val capture = ApplicationAudioCapture.createCapture(this.sourceInfo.sampleRate, this.sourceInfo.channels.toInt()) { frame ->
        frame.order(ByteOrder.LITTLE_ENDIAN)
        val floatBuffer = frame.asFloatBuffer()
        val array = FloatArray(floatBuffer.remaining())
        floatBuffer.get(array)

        if (AudioHelper.hasSpeech(array, AudioHelper.SPEECH_THRESHOLD)) {
            this.buffer += AudioHelper.getMonoAudio(
                AudioHelper.downsampleAnyTo16k(array, this.sourceInfo.sampleRate),
                this.sourceInfo.channels.toInt()
            )
        }

//        if (this.tempBuffer.hasRemaining()) {
//            if (this.tempBuffer.remaining() > array.size)
//                this.tempBuffer.put(array)
//            else
//                this.tempBuffer.put(array, 0, this.tempBuffer.remaining())
//
//            if (!this.tempBuffer.hasRemaining()) {
//                val flip = this.tempBuffer.flip()
//                val arr = ByteArray(flip.remaining())
//                flip.get(arr)
//
//                Path("./test.pcm").writeBytes(arr, StandardOpenOption.CREATE, StandardOpenOption.WRITE)
//                exitProcess(0)
//            }
//        }
    }

    init {
        if (this.sourceInfo.processId != null)
            this.capture.capture(this.sourceInfo.processId!!)
        else if (this.sourceInfo.appId != null)
            this.capture.capture(this.sourceInfo.appId!!)
        else
            throw IllegalStateException("Application source ${this.sourceInfo} doesn't have a process ID or application ID!")
    }

    override val available: Int
        get() {
            return try {
                this.buffer.size
            } catch (_: NullPointerException) {
                0
            }
        }

    override fun sample(): FloatArray {
        val available = this.available
        if (AudioHelper.BUFFER_SIZE > available)
            throw IllegalStateException("Failed to read samples from source ${this.sourceInfo}! Capacity: ${AudioHelper.BUFFER_SIZE}, available: $available")

        val snapshot = this.buffer.snapshot()
        this.buffer.reset()

        return snapshot
    }

    override fun close() {
        super.close()
        this.capture.stop()
    }

    companion object {
        init {
            ApplicationAudioCapture.autoLoad()
        }

        fun listSources(): List<SourceInfo.Application> {
            if (ApplicationAudioCapture.isAvailable()) {
                return ApplicationAudioCapture.listAllSources().filterIsInstance<SourceInfo.Application>()
            }

            return emptyList()
        }
    }
}
