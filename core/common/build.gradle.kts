plugins { id("com.android.library"); id("org.jetbrains.kotlin.android") }
android { namespace = "com.troxzy.trxchess.core.common"; compileSdk = 37; defaultConfig { minSdk = 24 } }
dependencies { api(libs.coroutines); api(libs.coroutines.android); testImplementation(libs.junit) }
