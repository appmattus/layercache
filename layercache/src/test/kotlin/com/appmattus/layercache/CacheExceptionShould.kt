/*
 * Copyright 2017 Appmattus Limited
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.appmattus.layercache

import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.core.Is.`is`
import org.hamcrest.core.IsEqual.equalTo
import org.hamcrest.core.IsInstanceOf.instanceOf
import org.hamcrest.core.StringStartsWith
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExpectedException

class CacheExceptionShould {

    @get:Rule
    var thrown: ExpectedException = ExpectedException.none()

    @Test
    fun `throw exception when exceptions is null`() {
        // expect exception
        thrown.expect(IllegalArgumentException::class.java)
        thrown.expectMessage(StringStartsWith("Parameter specified as non-null is null"))

        // when exception list is null
        CacheException("hi", TestUtils.uninitialized())
    }

    @Test
    fun `throw exception when exceptions is empty`() {
        // expect exception
        thrown.expect(IllegalArgumentException::class.java)
        thrown.expectMessage(StringStartsWith("You must provide at least one Exception"))

        // when exception list is null
        CacheException("hi", listOf())
    }

    @Test
    fun `populate cause with the only exception`() {
        // given we wrap one exception
        val firstException = TestException()
        val exception = CacheException("hi", listOf(firstException))

        // then cause contains the first exception
        assertThat(exception, instanceOf(CacheException::class.java))
        assertThat(exception.cause, instanceOf(TestException::class.java))
        assertThat(exception.suppressed.size, `is`(0))
    }

    @Test
    fun `populate cause with first exception`() {
        // given we wrap two exceptions
        val firstException = IllegalStateException()
        val secondException = IllegalArgumentException()
        val exception = CacheException("hi", listOf(firstException, secondException))

        // then cause contains the first exception
        assertThat(exception, instanceOf(CacheException::class.java))
        assertThat(exception.cause, equalTo<Throwable>(firstException))
    }

    @Test
    fun `populate suppressed with second exception`() {
        // given we wrap two exceptions
        val firstException = IllegalStateException()
        val secondException = IllegalArgumentException()
        val exception = CacheException("hi", listOf(firstException, secondException))

        // then suppressed contains the second exception
        assertThat(exception.suppressed.asList(), equalTo(listOf<Throwable>(secondException)))
    }

    @Test
    fun `populate suppressed with second exception onwards`() {
        // given we wrap two exceptions
        val firstException = IllegalStateException()
        val secondException = IllegalArgumentException()
        val thirdException = TestException()
        val exception = CacheException("hi", listOf(firstException, secondException, thirdException))

        // then suppressed contains the second exception
        assertThat(exception.suppressed.asList(), equalTo(listOf<Throwable>(secondException, thirdException)))
    }
}
