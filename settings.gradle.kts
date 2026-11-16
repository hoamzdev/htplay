pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
        maven("https://jitpack.io")
        maven("https://maven.google.com/")
        maven("https://android-sdk.is.com/")
    }
}

rootProject.name = "movie"

// API configuration shared with the data module's generated BuildConfig.
gradle.extra["movieApiConfig"] = mapOf(
    "BASE_URL" to "https://phimapi.com/",
    "BASE_URL_IMAGE" to "https://phimimg.com/",
    "BASE_URL_IMAGE_FULL" to "https://phimapi.com/image.php?url=",
    "ENDPOINT_ALL_FILM" to "danh-sach/phim-moi-cap-nhat-v3",
    "ENDPOINT_FILM_SERIES" to "v1/api/danh-sach/{type_list}",
    "ENDPOINT_FILM_GENRES" to "v1/api/the-loai/{type_list}",
    "ENDPOINT_FILM_COUNTRY" to "v1/api/quoc-gia/{type_list}",
    "ENDPOINT_SEARCH_FILM" to "v1/api/tim-kiem",
    "ENDPOINT_MOVIE_DETAIL" to "phim/{slug}",
    "ENDPOINT_COUNTRY" to "quoc-gia",
    "ENDPOINT_GENRES" to "the-loai",
)

include(":app")
include(":data")
include(":domain")
