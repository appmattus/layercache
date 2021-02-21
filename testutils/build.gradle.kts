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

import java.util.Properties

plugins {
    kotlin("jvm")
}

apply(from = "$rootDir/gradle/scripts/jacoco.gradle.kts")

dependencies {
    api(project(":layercache"))
    api("androidx.annotation:annotation:${Versions.AndroidX.annotation}")
    api("androidx.test:core:${Versions.AndroidX.testCore}")
    api("junit:junit:${Versions.junit4}")
    api("org.mockito:mockito-core:${Versions.mockito}")
    /* Objenesis 3.1, a dependency of mockito, is broken in Android connected tests */
    api("org.objenesis:objenesis") {
        version {
            strictly(Versions.objenesis)
        }
    }
    api("com.nhaarman.mockitokotlin2:mockito-kotlin:${Versions.mockitoKotlin}")

    api("com.github.stefanbirkner:system-rules:${Versions.systemRules}")

    val localProps = Properties()
    val localPropsFile = File(rootDir, "local.properties")
    if (localPropsFile.exists()) {
        localProps.load(localPropsFile.inputStream())
        compileOnly(files("${localProps.getProperty("sdk.dir")}/platforms/android-29/android.jar"))
    } else {
        compileOnly(files("${System.getenv("ANDROID_HOME")}/platforms/android-29/android.jar"))
    }

    //noinspection GradleDependency
    implementation("org.bouncycastle:bcprov-jdk15on:${Versions.bouncyCastle}")
}

tasks.named("check") {
    finalizedBy(rootProject.tasks.named("detekt"))
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}
