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

plugins {
    id("com.android.library")
    kotlin("android")
}

apply(from = "$rootDir/gradle/scripts/jacoco-android.gradle.kts")
apply(from = "$rootDir/gradle/scripts/bintray.gradle.kts")
apply(from = "$rootDir/gradle/scripts/dokka-javadoc.gradle.kts")

android {
    compileSdkVersion(30)

    defaultConfig {
        minSdkVersion(19)
        targetSdkVersion(30)
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    buildTypes {
        getByName("debug") {
            multiDexEnabled = true
        }

        getByName("release") {
            isMinifyEnabled = false
        }
    }

    sourceSets {
        getByName("main").java.srcDirs("src/main/kotlin")
        getByName("test").java.srcDirs("src/test/kotlin")
        getByName("androidTest").java.srcDirs("src/androidTest/kotlin")
    }

    testBuildType = "debug"
    testOptions.unitTests.isIncludeAndroidResources = true
}

dependencies {
    api(project(":layercache-android"))
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.2.0")

    testImplementation(project(":testutils"))
    testImplementation("org.robolectric:robolectric:4.3.1")
    testImplementation("androidx.test.ext:junit-ktx:1.1.1")
    testImplementation("androidx.test:runner:1.2.0")
}

tasks.create<Jar>("sourcesJar") {
    archiveClassifier.set("sources")
    from(android.sourceSets["main"].java.srcDirs)
}
