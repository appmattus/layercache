/*
 * Copyright 2020 Appmattus Limited
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
import org.junit.Assert.assertThrows
import org.junit.Test

class CacheExceptionShould {

    @Test
    fun `throw exception when exceptions is null`() {
        // when exception list is null
        val throwable = assertThrows(NullPointerException::class.java) {
            CacheException("hi", TestUtils.uninitialized())
        }

        // expect exception
        assertThat(throwable.message, StringStartsWith("Parameter specified as non-null is null"))
    }

    @Test
    fun `throw exception when exceptions is empty`() {
        // when exception list is null
        val throwable = assertThrows(IllegalArgumentException::class.java) {
            CacheException("hi", listOf())
        }

        // expect exception
        assertThat(throwable.message, StringStartsWith("You must provide at least one Exception"))
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
