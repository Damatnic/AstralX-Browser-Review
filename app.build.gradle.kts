plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("kotlin-kapt")
    id("dagger.hilt.android.plugin")
    id("kotlin-parcelize")
}

android {
    namespace = "com.astralx.browser"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.astralx.browser"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "com.astralx.browser.HiltTestRunner"
        
        vectorDrawables {
            useSupportLibrary = true
        }
        
        // Enable 120Hz display support
        manifestPlaceholders["preferredRefreshRate"] = "120"
        
        // RenderScript support for blur effects
        renderscriptTargetApi = 34
        renderscriptSupportModeEnabled = true
    }


    buildTypes {
        debug {
            isMinifyEnabled = false
            isDebuggable = true
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
        
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            
            // Performance optimizations
            configure<com.android.build.gradle.internal.dsl.BuildType> {
                isProfileable = true
            }
        }
        
        create("benchmark") {
            initWith(getByName("release"))
            signingConfig = signingConfigs.getByName("debug")
            matchingFallbacks += listOf("release")
            isProfileable = true
        }
    }
    

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        
        // Enable core library desugaring for Java 8+ APIs
        isCoreLibraryDesugaringEnabled = true
    }

    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs += listOf(
            "-opt-in=kotlin.RequiresOptIn",
            "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi",
            "-opt-in=kotlinx.coroutines.FlowPreview",
            "-Xjvm-default=all"
        )
    }

    buildFeatures {
        compose = true
        viewBinding = true
        buildConfig = true
    }
    
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.4"
    }
    
    packagingOptions {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "/META-INF/DEPENDENCIES"
        }
    }
    
    bundle {
        language {
            enableSplit = true
        }
        density {
            enableSplit = true
        }
        abi {
            enableSplit = true
        }
    }

    splits {
        abi {
            isEnable = true
            reset()
            include("armeabi-v7a", "arm64-v8a", "x86", "x86_64")
            isUniversalApk = true
        }
    }
    
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            isReturnDefaultValues = true
        }
        
        animationsDisabled = true
    }
    
    lint {
        baseline = file("lint-baseline.xml")
        checkDependencies = true
        checkTestSources = true
        warningsAsErrors = true
        abortOnError = true
        
        disable += setOf("MissingTranslation", "ExtraTranslation")
    }
}

dependencies {
    // Core Android
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.activity:activity-compose:1.8.1")
    
    // Compose
    implementation(platform("androidx.compose:compose-bom:2023.10.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.animation:animation")
    implementation("androidx.compose.animation:animation-graphics")
    
    // WebView
    implementation("androidx.webkit:webkit:1.9.0")
    
    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.7.3")
    
    // Dependency Injection
    implementation("com.google.dagger:hilt-android:2.48")
    kapt("com.google.dagger:hilt-compiler:2.48")
    implementation("androidx.hilt:hilt-navigation-compose:1.1.0")
    
    // Navigation
    implementation("androidx.navigation:navigation-compose:2.7.5")
    
    // Media & Video
    implementation("androidx.media3:media3-exoplayer:1.2.0")
    implementation("androidx.media3:media3-ui:1.2.0")
    implementation("androidx.media3:media3-exoplayer-hls:1.2.0")
    implementation("androidx.media3:media3-exoplayer-dash:1.2.0")
    implementation("androidx.media3:media3-datasource-okhttp:1.2.0")
    
    // Image Loading
    implementation("io.coil-kt:coil-compose:2.5.0")
    implementation("io.coil-kt:coil-gif:2.5.0")
    implementation("io.coil-kt:coil-video:2.5.0")
    
    // Network
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    
    // Database
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    kapt("androidx.room:room-compiler:2.6.1")
    
    // Security & Encryption
    implementation("androidx.security:security-crypto:1.1.0-alpha06")
    implementation("androidx.biometric:biometric:1.1.0")
    
    // Performance Monitoring
    implementation("androidx.metrics:metrics-performance:1.0.0-beta01")
    implementation("androidx.profileinstaller:profileinstaller:1.3.1")
    implementation("androidx.benchmark:benchmark-junit4:1.2.1")
    
    // Animation & Physics
    implementation("androidx.dynamicanimation:dynamicanimation:1.0.0")
    implementation("com.airbnb.android:lottie-compose:6.1.0")
    implementation("com.airbnb.android:lottie:6.2.0")
    
    // Material Design 3 (additional)
    implementation("com.google.android.material:material:1.11.0")
    
    // Circular Progress
    implementation("com.github.lzyzsd:circleprogress:1.2.1")
    
    // Google Cast
    implementation("com.google.android.gms:play-services-cast-framework:21.4.0")
    
    // Haptics
    implementation("androidx.core:core-haptics:1.0.0-alpha01")
    
    // Audio Processing
    implementation("com.arthenica:mobile-ffmpeg-audio:4.4.LTS")
    
    // Downloads
    implementation("androidx.work:work-runtime-ktx:2.9.0")
    implementation("com.github.tonyofrancis.Fetch2:xfetch2:3.1.6")
    
    // Gesture Detection
    implementation("com.github.GrenderG:Detectify:1.0.1")
    
    // Custom Views
    implementation("com.github.chrisbanes:PhotoView:2.3.0")
    implementation("com.github.MikeOrtiz:TouchImageView:3.6")
    
    // Blur & Visual Effects
    implementation("com.github.Dimezis:BlurView:version-2.0.3")
    implementation("jp.wasabeef:glide-transformations:4.3.0")
    
    // Permissions
    implementation("com.guolindev.permissionx:permissionx:1.7.1")
    
    // Analytics (Optional)
    implementation("com.google.firebase:firebase-analytics-ktx:21.5.0")
    implementation("com.google.firebase:firebase-crashlytics-ktx:18.6.0")
    
    // Testing
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.mockito:mockito-core:5.7.0")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    testImplementation("app.cash.turbine:turbine:1.0.0")
    
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    androidTestImplementation("androidx.benchmark:benchmark-macro-junit4:1.2.1")
    
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
    debugImplementation("com.squareup.leakcanary:leakcanary-android:2.12")
    
    // Core Library Desugaring
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.4")
    
    // ML Kit for on-device prediction
    implementation("com.google.mlkit:common:18.9.0")
    implementation("com.google.mlkit:smart-reply:17.0.2")
    
    // TensorFlow Lite for predictive caching
    implementation("org.tensorflow:tensorflow-lite:2.14.0")
    implementation("org.tensorflow:tensorflow-lite-support:0.4.4")
    
    // Custom font support
    implementation("androidx.compose.ui:ui-text-google-fonts:1.5.4")
    
    // Shimmer effect
    implementation("com.facebook.shimmer:shimmer:0.5.0")
    
    // Page indicator for thumbnails
    implementation("com.tbuonomo:dotsindicator:5.0")
    
    // Pull to refresh
    implementation("com.google.accompanist:accompanist-swiperefresh:0.32.0")
    
    // System UI Controller
    implementation("com.google.accompanist:accompanist-systemuicontroller:0.32.0")
    
    // Adaptive layouts
    implementation("androidx.compose.material3:material3-window-size-class:1.1.2")
}

// Performance optimization
tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    kotlinOptions {
        // Enable experimental APIs
        freeCompilerArgs += listOf(
            "-Xopt-in=kotlin.RequiresOptIn",
            "-Xopt-in=kotlin.Experimental",
            "-XXLanguage:+InlineClasses"
        )
        
        // Performance optimizations
        freeCompilerArgs += listOf(
            "-Xbackend-threads=0", // Use all available cores
            "-Xir-optimizations-after-inlining",
            "-Xskip-metadata-version-check"
        )
    }
} 