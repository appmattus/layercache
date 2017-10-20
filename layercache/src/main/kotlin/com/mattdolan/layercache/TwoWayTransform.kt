package com.mattdolan.layercache

/**
 * Represents a two-way transformation.
 */
interface TwoWayTransform<Value, MappedValue> : OneWayTransform<Value, MappedValue> {
    /**
     * Inverse of transform. Transform type from MappedValue to Value.
     */
    fun inverseTransform(mappedValue: MappedValue): Value
}
