package xyz.bluspring.unitytranslate.api.v2.download

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import org.jetbrains.annotations.ApiStatus
import xyz.bluspring.unitytranslate.api.v2.event.Event
import java.net.URL
import java.nio.file.Path

/**
 * Provides information towards the current download.
 */
sealed interface DownloadInfo {
    /**
     * The file path of the download.
     */
    val path: Path

    /**
     * The URL of the download.
     */
    val url: URL

    /**
     * Total amount of data downloaded, in bytes.
     */
    val downloadedBytes: Long

    /**
     * Total amount of data to download, in bytes.
     * This may return -1, which indicates that the final file size is unknown.
     */
    val totalBytes: Long

    /**
     * The remaining amount of data left to download, in bytes.
     * This may return -1 if [totalBytes] is -1, which indicates that the final file size is unknown.
     * If [deferred] is completed, this value will automatically return 0.
     */
    val remainingBytes: Long
        get() {
            if (isComplete)
                return 0

            if (totalBytes < 0)
                return -1

            return totalBytes - downloadedBytes
        }

    /**
     * The current progress of the download, in a value between [0.0,1.0].
     * If [totalBytes] is -1, the progress will be at 0.0 unless [deferred] is completed.
     */
    val progress: Double
        get() {
            if (isComplete)
                return 1.0

            if (totalBytes <= 0)
                return 0.0

            return (downloadedBytes.toDouble() / totalBytes.toDouble())
        }

    /**
     * A [CompletableDeferred] to attach to for automatically detecting when the download is completed.
     * It is highly recommended to use this for detecting a completed download, and only use the other values as information to display to the user.
     *
     * Do not call complete on this value manually. This must be handled by the downloader itself.
     */
    val deferred: Deferred<Unit>

    /**
     * Returns as completed if the [deferred] is completed.
     */
    val isComplete: Boolean
        get() = deferred.isCompleted && !deferred.isCancelled

    /**
     * Has the download started already?
     * Returns true if the
     */
    val hasStarted: Boolean

    /**
     * Represents an event that will get called when this download starts.
     */
    val onStartDownload: Event<Runnable>

    /**
     * Represents an event that will get called when this download finishes. Whether it completed in a failure or not is handled by [deferred].
     */
    val onFinishDownload: Event<Runnable>

    @ApiStatus.Internal
    open class Mutable(override val path: Path, override val url: URL) : DownloadInfo {
        override var downloadedBytes: Long = 0
            internal set

        override var totalBytes: Long = 0
            internal set

        override lateinit var deferred: Deferred<Unit>
            internal set

        override var hasStarted: Boolean = false

        override val isComplete: Boolean
            get() = ::deferred.isInitialized && super.isComplete

        override val onStartDownload: Event<Runnable> = Event(Runnable::class.java) { values ->
            Runnable {
                for (runnable in values) {
                    runnable.run()
                }
            }
        }

        override val onFinishDownload: Event<Runnable> = Event(Runnable::class.java) { values ->
            Runnable {
                for (runnable in values) {
                    runnable.run()
                }
            }
        }
    }
}
