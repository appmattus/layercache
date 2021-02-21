/*
 * Copyright 2021 Appmattus Limited
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

plugins {
    kotlin("jvm")
    id("com.vanniktech.maven.publish")
    id("org.jetbrains.dokka")
}

apply(from = "$rootDir/gradle/scripts/jacoco.gradle.kts")

dependencies {
    api(project(":layercache"))
    implementation("org.ehcache:ehcache:${Versions.ehcache}")

    testImplementation(project(":testutils"))
    testImplementation("org.slf4j:slf4j-nop:${Versions.slf4j}")
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}
