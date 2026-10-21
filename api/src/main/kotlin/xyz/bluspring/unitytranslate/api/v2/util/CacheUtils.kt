package xyz.bluspring.unitytranslate.api.v2.util

import java.util.*
import java.util.concurrent.ConcurrentHashMap

/**
 * A nice helper for caching stuff that might be getting called frequently.
 */
object CacheUtils {
    private val cacheLookups = ConcurrentHashMap<Pair<Class<*>, Class<*>>, Map<*, *>>()

    fun <T, U> createCacheLookup(first: Class<T>, second: Class<U>): MutableMap<T, U> {
        return this.cacheLookups.computeIfAbsent(first to second) {
            WeakHashMap<T, U>()
        } as MutableMap<T, U>
    }

    inline fun <reified T : Any, reified U : Any> T.buildOrCached(builder: T.() -> U): U {
        val cache = createCacheLookup(T::class.java, U::class.java)
        synchronized(cache) {
            var value = cache[this]
            if (value == null) {
                value = builder(this)
                cache[this] = value
            }

            return value
        }
    }
}
