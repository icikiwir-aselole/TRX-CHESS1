plugins { id("com.android.library"); id("org.jetbrains.kotlin.android"); id("com.google.devtools.ksp") }
android { namespace = "com.troxzy.trxchess.data"; compileSdk = 37; defaultConfig { minSdk = 24 } }
dependencies {
    implementation(project(":chess")); api(libs.androidx.room); implementation(libs.androidx.room.ktx); api(libs.androidx.datastore); implementation(libs.coroutines); ksp(libs.androidx.room.compiler); testImplementation(libs.junit)
}

ksp { arg("room.schemaLocation", "$projectDir/schemas") }
