package com.mattdolan.layercache

/**
 * A cache exception that can hold multiple causes. The first exception is available in cause and subsequent exceptions
 * in suppressed
 */
class CacheException(message: String, innerExceptions: List<Throwable>) : Exception(message) {
    init {
        require(innerExceptions.isNotEmpty(), { "You must provide at least one Exception" })

        initCause(innerExceptions.first())

        val hasAddSuppressed: Boolean = try {
            Throwable::class.java.getDeclaredMethod("addSuppressed", Throwable::class.java) != null
        } catch (e: NoSuchMethodException) {
            false
        } catch (e: SecurityException) {
            false
        }

        if (hasAddSuppressed) {
            innerExceptions.subList(1, innerExceptions.size).forEach { addSuppressed(it) }
        }
    }
}
