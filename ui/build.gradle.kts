plugins { id("com.android.library"); id("org.jetbrains.kotlin.android") }
android { namespace = "com.troxzy.trxchess.ui"; compileSdk = 37; defaultConfig { minSdk = 24 } }
dependencies { api(project(":chess")); api(project(":analysis:coordinator")); implementation(libs.androidx.core); implementation(libs.androidx.lifecycle); implementation(libs.androidx.activity); implementation(libs.coroutines.android) }
