#!/bin/bash
# Add work-runtime-ktx and hilt-work to libs.versions.toml
sed -i '' '/\[versions\]/a\
work = "2.9.0"\
hiltWork = "1.2.0"\
' gradle/libs.versions.toml

sed -i '' '/\[libraries\]/a\
androidx-work-runtime-ktx = { group = "androidx.work", name = "work-runtime-ktx", version.ref = "work" }\
androidx-hilt-work = { group = "androidx.hilt", name = "hilt-work", version.ref = "hiltWork" }\
androidx-hilt-compiler = { group = "androidx.hilt", name = "hilt-compiler", version.ref = "hiltWork" }\
' gradle/libs.versions.toml

# Add to app/build.gradle.kts
sed -i '' '/implementation(libs.hilt.android)/a\
    implementation(libs.androidx.work.runtime.ktx)\
    implementation(libs.androidx.hilt.work)\
    ksp(libs.androidx.hilt.compiler)\
' app/build.gradle.kts
