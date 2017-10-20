package com.mattdolan.layercache

import org.junit.Assert.fail
import org.junit.rules.ExternalResource
import java.util.concurrent.atomic.AtomicInteger

class ExecutionExpectation : ExternalResource() {
    private val integer = AtomicInteger(0)
    private var target = 0

    fun execute() {
        integer.incrementAndGet()
    }

    fun expect(target: Int) {
        this.target = target
        //integer.set(target)
    }

    override fun after() {
        val actual = integer.get()

        if (target == actual) {
            return
        }

        fail("Expected test to make ${target} executions but actually made ${actual}")
    }
}
