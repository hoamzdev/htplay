plugins {
    alias(libs.plugins.android.application)
    id("kotlin-parcelize")
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    id("com.google.gms.google-services") version "4.5.0"
}

android {
    namespace = "com.htdev.movie.htplay"
    compileSdk {
        version = release(37)
    }

    buildFeatures {
        buildConfig = true
    }

    defaultConfig {
        applicationId = "com.htdev.movie.htplay"
        minSdk = 24
        targetSdk = 37
        versionCode = 100
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    viewBinding {
        enable = true
    }

    buildTypes {
        debug {
            // Debug: không tối ưu, build nhanh, dễ debug
            optimization {
                enable = false
            }
        }
        release {
            // Release (CH Play): bật R8 để giảm dung lượng, khó decompile
            optimization {
                enable = true
                keepRules {
                    files.add(getDefaultProguardFile("proguard-android-optimize.txt"))
                    files.add(File("proguard-rules.pro"))
                }
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(libs.androidx.activity.ktx)
    // AndroidX / UI
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.fragment)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    implementation(libs.googleid)
    implementation(libs.firebase.auth)
    implementation(libs.androidx.credentials.play.services.auth)
    implementation(libs.material)

    implementation(libs.blurview.vversion310)

    implementation(project(":data"))
    implementation(project(":domain"))

    // Test
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)

    // Dimens
    implementation(libs.dimenlib)
    implementation(libs.dimensplib)

    // Rounded ImageView
    implementation(libs.roundedimageview)

    // Image
    implementation(libs.glide)

    // Network
    implementation(libs.retrofit)
    implementation(libs.converter.gson)
    implementation(libs.logging.interceptor)

    // Room
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // Json
    implementation(libs.gson)

    // StoriesRecyclerView

    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.exoplayer.hls)
    implementation(libs.androidx.media3.ui)
    implementation(libs.androidx.media3.database)
    implementation(libs.koin.android)

    // ViewModel and LiveData
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.process)

    // Lottie
    implementation(libs.lottie)

    implementation(libs.facebook.shimmer)
    implementation(libs.androidx.multidex)
    implementation(libs.androidx.work.runtime.ktx)

    // Hilt
    implementation(libs.javax.inject)
    implementation(libs.hilt.android)
    ksp(libs.hilt.android.compiler)

    implementation(libs.androidx.fragment.ktx)
}
