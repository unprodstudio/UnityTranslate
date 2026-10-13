package xyz.bluspring.unitytranslate.api.v2.util

import kotlinx.coroutines.Deferred
import kotlinx.coroutines.ExperimentalCoroutinesApi

@OptIn(ExperimentalCoroutinesApi::class)
fun <T> Deferred<T>.onComplete(handler: (T) -> Unit) {
    this.invokeOnCompletion {
        if (it == null) {
            handler(this.getCompleted())
        }
    }
}
