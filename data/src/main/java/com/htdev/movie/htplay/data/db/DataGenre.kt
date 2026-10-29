package com.htdev.movie.htplay.data.db

import com.htdev.movie.htplay.data.common.Api
import com.htdev.movie.htplay.domain.entities.Country
import com.htdev.movie.htplay.domain.entities.Genre

/**
 * @author hwa..
 */

object DataGenre {
    val genres = listOf(
        Genre(
            name = genreSlugToEnglish("hanh-dong"),
            slug = "hanh-dong",
            poster = "${Api.BASE_URL_IMAGE}/upload/vod/20250313-1/531372020d2f804a76c5913dad1517fa.jpg"
        ),
        Genre(
            name = genreSlugToEnglish("tinh-cam"),
            slug = "tinh-cam",
            poster = "${Api.BASE_URL_IMAGE}/upload/vod/20260126-1/93eb0168e1ba7f3d070ebb1a6f0fcab7.jpg"
        ),
        Genre(
            name = genreSlugToEnglish("co-trang"),
            slug = "co-trang",
            poster = "${Api.BASE_URL_IMAGE}/upload/vod/20240516-1/e63204ec4c8e01363683674fa2eccf71.jpg"
        ),
        Genre(
            name = genreSlugToEnglish("tai-lieu"),
            slug = "tai-lieu",
            poster = "${Api.BASE_URL_IMAGE}/upload/vod/20260307-1/b259cd84e8b4d50edc1f8be3a79a4285.jpg"
        ),
        Genre(
            name = genreSlugToEnglish("bi-an"),
            slug = "bi-an",
            poster = "${Api.BASE_URL_IMAGE}/upload/vod/20260227-1/8b60d37846e87e2403abdaf5ec396b39.jpg"
        ),
        Genre(
            name = genreSlugToEnglish("tam-ly"),
            slug = "tam-ly",
            poster = "${Api.BASE_URL_IMAGE}/upload/vod/20260311-1/3a0ba6c2d1f9158fb44bc4a54fa9c378.jpg"
        ),
        Genre(
            name = genreSlugToEnglish("tre-em"),
            slug = "tre-em",
            poster = "${Api.BASE_URL_IMAGE}/upload/vod/20260228-1/880269004d7c97f22c21b9073695eae9.jpg"
        ),
        Genre(
            name = genreSlugToEnglish("phieu-luu"),
            slug = "phieu-luu",
            poster = "${Api.BASE_URL_IMAGE}/upload/vod/20260204-1/551fbd9cb8955022c0b50a60727decaa.jpg"
        ),
        Genre(
            name = genreSlugToEnglish("gia-dinh"),
            slug = "gia-dinh",
            poster = "${Api.BASE_URL_IMAGE}/upload/vod/20260310-1/7a8e1d8d2940db0b1f6b371cc423084e.jpg"
        ),
        Genre(
            name = genreSlugToEnglish("hinh-su"),
            slug = "hinh-su",
            poster = "${Api.BASE_URL_IMAGE}/upload/vod/20260203-1/7251f3abb5ced08c44f7c990993ebb3b.jpg"
        ),
        Genre(
            name = genreSlugToEnglish("hoc-duong"),
            slug = "hoc-duong",
            poster = "${Api.BASE_URL_IMAGE}/upload/vod/20260110-1/ddc421e82c6a035af9c7f2a95ab305b1.jpg"
        ),
        Genre(
            name = genreSlugToEnglish("hai-huoc"),
            slug = "hai-huoc",
            poster = "${Api.BASE_URL_IMAGE}/upload/vod/20241017-1/8ce53643bb316f8fb8dc3dddd955f2ce.jpg"
        )
    )

    val exploreCountries = listOf(
        Country(name = "Thái Lan", slug = "thai-lan"),
        Country(name = "Hồng Kông", slug = "hong-kong"),
        Country(name = "Ấn Độ", slug = "an-do"),
        Country(name = "Canada", slug = "canada"),
        Country(name = "Tây Ban Nha", slug = "tay-ban-nha"),
        Country(name = "Ba Lan", slug = "ba-lan"),
        Country(name = "Đài Loan", slug = "dai-loan"),
        Country(name = "Anh", slug = "anh"),
        Country(name = "Nga", slug = "nga"),
        Country(name = "Úc", slug = "uc"),
        Country(name = "Ý", slug = "y"),
        Country(name = "Na Uy", slug = "na-uy"),
    ).map { country -> country.copy(name = getEnglishCountryName(country.name)) }

}

fun getEnglishCountryName(vietnameseName: String): String {
    return when(vietnameseName) {
        "Thái Lan" -> "Thailand"
        "Hồng Kông" -> "Hong Kong"
        "Ấn Độ" -> "India"
        "Canada" -> "Canada"
        "Tây Ban Nha" -> "Spain"
        "Ba Lan" -> "Poland"
        "Đài Loan" -> "Taiwan"
        "Anh" -> "UK"
        "Nga" -> "Russia"
        "Úc" -> "Australia"
        else -> "Italia"
    }
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

