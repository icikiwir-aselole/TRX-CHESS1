plugins { id("com.android.library"); id("org.jetbrains.kotlin.android") }
android { namespace = "com.troxzy.trxchess.benchmark"; compileSdk = 37; defaultConfig { minSdk = 24 } }
dependencies { api(project(":engine:api")); api(project(":chess")); implementation(libs.coroutines) }
