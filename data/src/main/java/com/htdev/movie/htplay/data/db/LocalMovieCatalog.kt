package com.htdev.movie.htplay.data.db

import com.htdev.movie.htplay.data.common.Api
import com.htdev.movie.htplay.domain.entities.Country
import com.htdev.movie.htplay.domain.entities.Genre

/**
 * @author hwa..
 */

object LocalMovieCatalog {
    val genres = listOf(
        Genre(
            name = genreSlugToEnglish("hanh-dong"),
            slug = "hanh-dong",
            poster = "${Api.BASE_URL_IMAGE}uploads/movies/20261004/huyet-quy-hon-chien-phan-2-poster.webp"
        ),
        Genre(
            name = genreSlugToEnglish("tinh-cam"),
            slug = "tinh-cam",
            poster = "${Api.BASE_URL_IMAGE}uploads/movies/20261021/van-co-cua-co-ay-poster.webp"
        ),
        Genre(
            name = genreSlugToEnglish("co-trang"),
            slug = "co-trang",
            poster = "${Api.BASE_URL_IMAGE}upload/vod/20240516-1/e63204ec4c8e01363683674fa2eccf71.jpg"
        ),
        Genre(
            name = genreSlugToEnglish("bi-an"),
            slug = "bi-an",
            poster = "${Api.BASE_URL_IMAGE}upload/vod/20260227-1/8b60d37846e87e2403abdaf5ec396b39.jpg"
        ),
        Genre(
            name = genreSlugToEnglish("tre-em"),
            slug = "tre-em",
            poster = "${Api.BASE_URL_IMAGE}uploads/movies/20261014/horror-collector-kyoufu-collector-poster.webp"
        ),
        Genre(
            name = genreSlugToEnglish("phieu-luu"),
            slug = "phieu-luu",
            poster = "${Api.BASE_URL_IMAGE}uploads/movies/20261012/dragon-ball-super-beerus-poster.webp"
        ),
        Genre(
            name = genreSlugToEnglish("gia-dinh"),
            slug = "gia-dinh",
            poster = "${Api.BASE_URL_IMAGE}upload/vod/20260310-1/7a8e1d8d2940db0b1f6b371cc423084e.jpg"
        ),
        Genre(
            name = genreSlugToEnglish("hinh-su"),
            slug = "hinh-su",
            poster = "${Api.BASE_URL_IMAGE}upload/vod/20260203-1/7251f3abb5ced08c44f7c990993ebb3b.jpg"
        ),
        Genre(
            name = genreSlugToEnglish("hai-huoc"),
            slug = "hai-huoc",
            poster = "${Api.BASE_URL_IMAGE}uploads/movies/20261030/dhindora-phan-1-poster.webp"
        )
    )

    val exploreCountries = listOf(
        Country(name = "Thailand", slug = "thai-lan"),
        Country(name = "Hong Kong", slug = "hong-kong"),
        Country(name = "India", slug = "an-do"),
        Country(name = "Canada", slug = "canada"),
        Country(name = "Spain", slug = "tay-ban-nha"),
        Country(name = "Poland", slug = "ba-lan"),
        Country(name = "Taiwan", slug = "dai-loan"),
        Country(name = "UK", slug = "anh"),
        Country(name = "Russia", slug = "nga"),
        Country(name = "Australia", slug = "uc"),
        Country(name = "Italy", slug = "y"),
        Country(name = "Norway", slug = "na-uy"),
    )
}

fun genreSlugToEnglish(slug: String): String {
    return when (slug) {
        "hanh-dong" -> "Action"
        "mien-tay" -> "Western"
        "tre-em" -> "Kids"
        "lich-su" -> "History"
        "co-trang" -> "Costume"
        "chien-tranh" -> "War"
        "vien-tuong" -> "Sci-Fi"
        "kinh-di" -> "Horror"
        "tai-lieu" -> "Documentary"
        "bi-an" -> "Mystery"
        "phim-18" -> "Adult"
        "tinh-cam" -> "Romance"
        "tam-ly" -> "Psychological"
        "the-thao" -> "Sports"
        "phieu-luu" -> "Adventure"
        "am-nhac" -> "Music"
        "gia-dinh" -> "Family"
        "hoc-duong" -> "School"
        "hai-huoc" -> "Comedy"
        "hinh-su" -> "Crime"
        "vo-thuat" -> "Martial Arts"
        "khoa-hoc" -> "Science"
        "than-thoai" -> "Mythology"
        "chinh-kich" -> "Drama"
        "kinh-dien" -> "Classic"
        "phim-ngan" -> "Short Film"
        else -> slug
    }
}

