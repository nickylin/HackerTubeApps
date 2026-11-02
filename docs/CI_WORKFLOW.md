# CI workflow (one-time manual add)

GitHub blocks both API tokens used by the maintainer's automation from creating
files under `.github/workflows/` (they lack the `workflow` permission), so this
file has to be added once by a human in the web UI:

1. Open https://github.com/nickylin/HackerTubeApps/new/main/.github/workflows
2. Name the file `build.yml`, paste the content below, and commit.

After that, every push builds debug + release APKs and uploads them as artifacts.

```yaml
name: Build

on:
  push:
    branches: [main]
  pull_request:
  workflow_dispatch:

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - name: Set up JDK 17
        uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: "17"
      - name: Set up Android SDK
        uses: android-actions/setup-android@v3
      - name: Build debug and release APKs
        run: ./gradlew assembleDebug assembleRelease --stacktrace
      - name: Upload APKs
        uses: actions/upload-artifact@v4
        with:
          name: hackertube-apks
          path: |
            app/build/outputs/apk/debug/*.apk
            app/build/outputs/apk/release/*.apk
```
