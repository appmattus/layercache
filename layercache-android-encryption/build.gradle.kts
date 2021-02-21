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
    id("com.android.library")
    kotlin("android")
    id("com.vanniktech.maven.publish")
    id("org.jetbrains.dokka")
}

apply(from = "$rootDir/gradle/scripts/jacoco-android.gradle.kts")

android {
    compileSdkVersion(30)

    defaultConfig {
        minSdkVersion(21)
        targetSdkVersion(30)
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "com.appmattus.layercache.AndroidXJUnitRunner"
        testInstrumentationRunnerArguments["notPackage"] = "org.bouncycastle"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    buildTypes {
        getByName("debug") {
            multiDexEnabled = true
            multiDexKeepProguard = file("multidex-config.pro")
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
    compileOnly("androidx.annotation:annotation:${Versions.AndroidX.annotation}")

    implementation("com.google.crypto.tink:tink-android:${Versions.Google.tink}")

    androidTestImplementation(project(":testutils"))
    androidTestImplementation("androidx.test.ext:junit-ktx:${Versions.AndroidX.testExtJunit}")
    androidTestImplementation("androidx.test:runner:${Versions.AndroidX.testRunner}")
    androidTestImplementation("androidx.multidex:multidex:${Versions.AndroidX.multidex}")
}
