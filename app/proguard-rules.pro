# R8 / ProGuard rules for the release build.
#
# R8 is enabled for release via isMinifyEnabled = true in build.gradle.kts. The library consumer
# rules that ship with AndroidX, Jetpack Compose, Room, CameraX, ML Kit, and the Kotlin coroutines
# runtime are applied automatically, and org.json is part of the Android platform (never shrunk).
#
# Add keep rules below ONLY if a release build (./gradlew :app:assembleRelease) actually reports a
# missing or stripped class. R8 writes suggested rules to
# app/build/outputs/mapping/release/missing_rules.txt when it needs them -- copy the relevant ones
# here rather than adding rules speculatively.
