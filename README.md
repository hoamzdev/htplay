# HTPLAY
# Movie Streaming App
# Created by htdev

## Local API configuration

Copy `api.local.properties.example` to `api.local.properties` in the project root and fill in every value before syncing Gradle. The base URL must end with `/`; keep `{type_list}` and `{slug}` in the endpoint paths where indicated in the template.

`api.local.properties` is ignored by Git. The `data` module reads it to generate `BuildConfig` fields. Only the empty example file should be committed. A fresh clone or CI build needs its own copy of the local configuration.
