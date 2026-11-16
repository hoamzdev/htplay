import java.io.StringReader
import java.util.Properties

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

val apiConfigFile = rootProject.layout.projectDirectory.file("api.local.properties")
val apiConfigText = providers.fileContents(apiConfigFile).asText.orNull
    ?: throw GradleException("Missing api.local.properties. Copy api.local.properties.example and fill in your API configuration.")
val apiProperties = Properties().apply { load(StringReader(apiConfigText)) }
val movieApiConfig = listOf(
    "BASE_URL",
    "BASE_URL_IMAGE",
    "BASE_URL_IMAGE_FULL",
    "ENDPOINT_ALL_FILM",
    "ENDPOINT_FILM_SERIES",
    "ENDPOINT_FILM_GENRES",
    "ENDPOINT_FILM_COUNTRY",
    "ENDPOINT_SEARCH_FILM",
    "ENDPOINT_MOVIE_DETAIL",
    "ENDPOINT_COUNTRY",
    "ENDPOINT_GENRES",
).associateWith { key ->
    apiProperties.getProperty(key)?.trim()?.takeIf { it.isNotEmpty() }
        ?: throw GradleException("Missing $key in api.local.properties.")
}

android {
    namespace = "com.htdev.movie.htplay.data"
    buildFeatures {
        buildConfig = true
    }
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        minSdk = 24
        consumerProguardFiles("consumer-rules.keep")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        //noinspection WrongGradleMethod
        movieApiConfig.forEach { (name, value) ->
            val escapedValue = value.replace("\\", "\\\\").replace("\"", "\\\"")
            buildConfigField("String", name, "\"$escapedValue\"")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    // Room
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    implementation(libs.androidx.core.ktx)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.play.services)
    implementation(libs.androidx.paging.common)
    implementation(libs.hilt.android)
    ksp(libs.hilt.android.compiler)
    implementation(libs.retrofit)
    implementation(libs.converter.gson)
    implementation(libs.gson)
    implementation(libs.androidx.credentials)
    implementation(libs.googleid)
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    implementation(project(":domain"))
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
}
