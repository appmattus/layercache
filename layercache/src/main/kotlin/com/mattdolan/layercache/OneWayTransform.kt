package com.mattdolan.layercache

/**
 * Represents a one-way transformation.
 */
interface OneWayTransform<Value, MappedValue> {
    /**
     * Transform type from Value to MappedValue.
     */
    fun transform(value: Value): MappedValue
}
