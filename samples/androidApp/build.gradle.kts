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
    id("com.android.application")
    kotlin("android")
    id("kotlin-parcelize")
    kotlin("kapt")
    id("androidx.navigation.safeargs.kotlin")
}

apply(plugin = "dagger.hilt.android.plugin")

dependencies {

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:${Versions.coroutines}")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:${Versions.coroutines}")

    // Architecture
    implementation("androidx.fragment:fragment-ktx:${Versions.AndroidX.fragment}")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:${Versions.AndroidX.lifecycle}")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:${Versions.AndroidX.lifecycle}")
    implementation("androidx.navigation:navigation-fragment-ktx:${Versions.AndroidX.navigation}")
    implementation("androidx.navigation:navigation-ui-ktx:${Versions.AndroidX.navigation}")
    implementation("org.orbit-mvi:orbit-viewmodel:${Versions.orbitMvi}")

    // UI
    implementation("com.google.android.material:material:${Versions.Google.material}")
    implementation("androidx.appcompat:appcompat:${Versions.AndroidX.appCompat}")
    implementation("androidx.constraintlayout:constraintlayout:${Versions.AndroidX.constraintLayout}")
    implementation("androidx.vectordrawable:vectordrawable:${Versions.AndroidX.vectorDrawable}")
    implementation("com.xwray:groupie:${Versions.groupie}")
    implementation("com.xwray:groupie-viewbinding:${Versions.groupie}")
    implementation("io.coil-kt:coil-svg:${Versions.coil}")

    // Memory leak detection and fixes
    debugImplementation("com.squareup.leakcanary:leakcanary-android:${Versions.leakCanary}")
    implementation("com.squareup.leakcanary:plumber-android:${Versions.leakCanary}")

    // Dependency Injection
    implementation("androidx.hilt:hilt-lifecycle-viewmodel:${Versions.AndroidX.hilt}")
    kapt("androidx.hilt:hilt-compiler:${Versions.AndroidX.hilt}")
    implementation("com.google.dagger:hilt-android:${Versions.Google.dagger}")
    kapt("com.google.dagger:hilt-android-compiler:${Versions.Google.dagger}")

    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:${Versions.desugar}")
}

android {
    compileSdkVersion(30)
    defaultConfig {
        applicationId = "com.appmattus.layercache.samples"
        minSdkVersion(21)
        targetSdkVersion(30)
        versionCode = 1
        versionName = "1.0"
        vectorDrawables.useSupportLibrary = true
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        isCoreLibraryDesugaringEnabled = true

        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_1_8.toString()
    }

    buildFeatures {
        buildConfig = true
        viewBinding = true
    }

    sourceSets.all {
        java.srcDir("src/$name/kotlin")
    }
}
