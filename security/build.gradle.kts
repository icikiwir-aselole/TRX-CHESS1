plugins { id("com.android.library"); id("org.jetbrains.kotlin.android") }
android { namespace = "com.troxzy.trxchess.security"; compileSdk = 37; defaultConfig { minSdk = 24 } }
dependencies { api(libs.androidx.core); implementation(libs.androidx.datastore); testImplementation(libs.junit) }
