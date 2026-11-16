# HTPlay

**Your next movie night, wherever you are.**

HTPlay is an Android movie app for discovering new stories, watching your favorites, and keeping movies ready for offline viewing. From browsing the latest releases to picking up an unfinished episode, HTPlay brings your movie collection together in one place.

## Discover something to watch

Explore movies by genre and country, browse the latest releases, watch trailers, or search for a specific title. Choose your favorite genres to find recommendations that match your interests.

## Watch your way

- **Pick up where you left off** with saved episodes and playback progress.
- **Enjoy fullscreen playback** and switch between available episodes.
- **Keep the story going** with optional automatic playback of the next episode.
- **Use the mini player** while exploring the app, or enable picture-in-picture when leaving it.

## Take your movies with you

Download episodes for offline viewing, with movie titles and posters kept in your Downloads library. Search your downloaded collection, play a saved movie, or remove it when you need more space.

Download notifications show the movie poster, episode, progress bar, and percentage. Tap a notification to open your Downloads.

If you are already signed in, opening HTPlay without an internet connection takes you straight to your downloaded movies.

## Make it yours

Save movies to your favorites and build a watchlist for later. Sign in with Google or email, or continue as a guest. Adjust notifications, playback preferences, and the app language in Settings.

**Available in 8 languages:** English, Vietnamese, French, German, Italian, Chinese, Japanese, and Korean.

## Tech stack

| Area | Technologies |
| --- | --- |
| Platform | Native Android, Android 7.0+ (API 24+) |
| Language | Kotlin |
| UI | XML layouts, View Binding, Material Components, ConstraintLayout, RecyclerView |
| Architecture | MVVM with separate presentation, domain, and data modules |
| State management | AndroidX ViewModel, StateFlow, Kotlin Coroutines |
| Navigation | AndroidX Navigation Component with a custom bottom tab bar |
| Dependency injection | Hilt / Dagger, KSP |
| Networking | Retrofit, OkHttp, Gson |
| Pagination | AndroidX Paging |
| Video playback | Media3 ExoPlayer, HLS streaming, fullscreen and picture-in-picture |
| Offline downloads | Media3 DownloadService, foreground service notifications, persistent media cache |
| Image loading | Glide with locally cached posters for offline viewing |
| Local storage | Room, SharedPreferences |
| Authentication | Firebase Authentication, Google Sign-In, Android Credential Manager |
| Cloud storage | Cloud Firestore for account-linked genre preferences |
| Build tools | Gradle Kotlin DSL, Android Gradle Plugin, version catalogs |
| Testing | JUnit, Kotlin Coroutines Test, AndroidX Test, Espresso |

## Architecture

HTPlay follows an MVVM structure with three modules:

| Module | Responsibility |
| --- | --- |
| `app` | Screens, ViewModels, navigation, playback UI, localization, and download notifications |
| `domain` | Domain models, repository interfaces, and application use cases |
| `data` | API services, response mapping, repository implementations, Room storage, and Firebase integration |

The UI observes ViewModel state through flows and delegates application actions to domain use cases. Repository implementations in the data module connect those use cases to remote services and local storage.

---
apk : https://drive.google.com/file/d/1QRTR8_64KLLrMAieFOOetNF5SBXM5upB/view?usp=sharing

Created by **htdev**.
