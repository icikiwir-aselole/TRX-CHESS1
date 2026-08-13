plugins { id("com.android.library"); id("org.jetbrains.kotlin.android") }
android { namespace = "com.troxzy.trxchess.overlay"; compileSdk = 37; defaultConfig { minSdk = 24 } }
dependencies { api(project(":analysis:coordinator")); implementation(libs.androidx.core); implementation(libs.coroutines.android) }
